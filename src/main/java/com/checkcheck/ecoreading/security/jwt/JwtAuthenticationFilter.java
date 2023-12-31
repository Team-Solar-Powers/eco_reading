package com.checkcheck.ecoreading.security.jwt;


import com.checkcheck.ecoreading.domain.loginHistory.service.LoginHistoryService;
import com.checkcheck.ecoreading.domain.users.entity.Users;
import com.checkcheck.ecoreading.domain.users.repository.UserRepository;
import com.checkcheck.ecoreading.domain.users.service.RedisService;
import com.checkcheck.ecoreading.domain.users.service.UserService;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends GenericFilterBean {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisTemplate;
    private final UserRepository userRepository;

    private final LoginHistoryService loginHistoryService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String accessToken = resolveToken(httpRequest, "accessToken");
        String refreshToken = resolveToken(httpRequest, "refreshToken");

        if (isTokenValid(accessToken)) {
            processValidAccessToken(accessToken, httpRequest);
        } else if (StringUtils.hasText(refreshToken) && isRefreshTokenValid(refreshToken, httpRequest)) {
            refreshTokenAndProcess(httpResponse, refreshToken, httpRequest);
        }
        chain.doFilter(request, response);
    }

    private boolean isTokenValid(String token) {
        return StringUtils.hasText(token) && jwtTokenProvider.validateToken(token);
    }

    private boolean isRefreshTokenValid(String accessToken, HttpServletRequest httpRequest) {
        String userEmail = jwtTokenProvider.getSubject(accessToken);
        String storedRefreshToken = redisTemplate.getValues("RefreshToken:" + userEmail);
        // storedRefreshToken이 null인 경우의 처리도 추가해야 할 수 있음
        return jwtTokenProvider.validateToken(storedRefreshToken);
    }

    private void processValidAccessToken(String accessToken, HttpServletRequest httpRequest) {
        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String userEmail = jwtTokenProvider.getSubject(accessToken);
//        Optional<Users> user = userRepository.findByEmail(userEmail);
//        user.ifPresent(u -> httpRequest.setAttribute("userId", u.getUsersId()));
    }

    private void refreshTokenAndProcess(HttpServletResponse httpResponse, String accessToken, HttpServletRequest httpRequest) {
        String userEmail = jwtTokenProvider.getSubject(accessToken);
        String refreshToken = redisTemplate.getValues("RefreshToken:" + userEmail);
        if (!StringUtils.hasText(refreshToken) || !isRefreshTokenValid(accessToken, httpRequest)) {
            logout(httpRequest, httpResponse);
            return;
        }
        // 나머지 로직은 기존과 동일
        Optional<Users> user = userRepository.findByEmail(userEmail);
        user.ifPresent(u -> httpRequest.setAttribute("userId", u.getUsersId()));
        if (user.isPresent()) {
            Long userId = user.get().getUsersId(); // userId 추출
            Collection<GrantedAuthority> authorities = jwtTokenProvider.getRoles(refreshToken);
            String newAccessToken = jwtTokenProvider.createAccessToken(userEmail, authorities, userId);
            addTokenToCookie(httpResponse, "accessToken", newAccessToken);
            processValidAccessToken(newAccessToken, httpRequest);
        }
    }

    private void addTokenToCookie(HttpServletResponse response, String name, String token) {
        Cookie cookie = new Cookie(name, token);
        // 쿠키 설정
        cookie.setHttpOnly(false);
        cookie.setPath("/");
        cookie.setSecure(false);
        cookie.setMaxAge((int) (30 * 60 * 1000L));
        // 필요한 경우 쿠키의 보안 설정을 여기에 추가
        response.addCookie(cookie);
    }

    private String resolveToken(HttpServletRequest request, String tokenName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (tokenName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    private void logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            updateLogoutTime(authentication);
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            deleteCookies(request, response, "accessToken", "refreshToken");
            if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                String userEmail = userDetails.getUsername();
                redisTemplate.deleteValues("RefreshToken:" + userEmail);
            } else if (authentication.getPrincipal() instanceof String) {
                String username = (String) authentication.getPrincipal();
                redisTemplate.deleteValues("RefreshToken:" + username);
            } else {
                throw new IllegalStateException("Unexpected type of principal object");
            }
        }
    }
    private void deleteCookies(HttpServletRequest request, HttpServletResponse response, String... cookieNames) {
        Arrays.asList(cookieNames).forEach(cookieName -> {
            Cookie cookie = new Cookie(cookieName, null);
            cookie.setHttpOnly(false);
            cookie.setSecure(false);
            cookie.setPath("/"); // 쿠키 경로를 설정합니다. 일반적으로 루트("/")를 사용합니다.
            cookie.setMaxAge(0); // 쿠키를 즉시 만료시킵니다.
            response.addCookie(cookie);
        });
    }

    private void updateLogoutTime(Authentication authentication) {
        // Authentication 객체에서 사용자 정보 추출
        String userEmail;
        if (authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            userEmail = userDetails.getUsername();
        } else if (authentication.getPrincipal() instanceof String) {
            userEmail = (String) authentication.getPrincipal();
        } else {
            throw new IllegalStateException("Unexpected type of principal object");
        }

        // 이메일 주소를 사용하여 Users 엔티티 조회
        Users user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + userEmail));

        // 로그아웃 시간 업데이트
        loginHistoryService.updateLogoutTime(user);
    }
}
