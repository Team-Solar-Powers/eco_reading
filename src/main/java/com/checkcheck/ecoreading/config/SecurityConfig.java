package com.checkcheck.ecoreading.config;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

import com.checkcheck.ecoreading.domain.loginHistory.service.LoginHistoryService;
import com.checkcheck.ecoreading.domain.users.exception.AuthenticationEmailException;
import com.checkcheck.ecoreading.domain.users.exception.CustomAuthenticationFailureHandler;
import com.checkcheck.ecoreading.domain.users.repository.UserRepository;
import com.checkcheck.ecoreading.domain.users.service.RedisService;
import com.checkcheck.ecoreading.domain.users.service.UserCustomDetailService;
import com.checkcheck.ecoreading.domain.users.service.UserOAuthCustomService;
import com.checkcheck.ecoreading.domain.users.service.UserService;
import com.checkcheck.ecoreading.security.jwt.JwtAuthenticationFilter;
import com.checkcheck.ecoreading.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

    private final UserCustomDetailService userDetailsService; // UserDetailsService 주입

    private final UserOAuthCustomService userOAuthCustomService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisTemplate;

    private final UserRepository repository;
    private final LoginHistoryService loginHistoryService;


    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().antMatchers(
                "/h2-console/**",
                "/api-document/**",
                "/swagger-ui/**",
                "/static/**",
                "/css/**",
                "/js/**",
                "/images/**"
        );
    }
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, redisTemplate, repository, loginHistoryService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeRequests(auth -> auth
                        //.antMatchers("/**").permitAll() // 모든 요청에 대해 접근을 허용
                        .antMatchers("/user/mypage/**").authenticated()
                        .antMatchers("/user/**").permitAll()
                        .antMatchers("/main/").permitAll()
                        .antMatchers("/admin/**").hasAuthority("ROLE_ADMIN")
                        .anyRequest().authenticated() // 나머지 요청은 모두 인증 필요
                )
                .formLogin().disable()
                // 403 에러 처리
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/user/403error")
                )
                .csrf(csrf -> csrf.disable()) // H2 콘솔 사용 시 CSRF 비활성화 필요
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/user/login")
                        .defaultSuccessUrl("/user/social/login", true)
                        //커스텀 핸들러 설정
                        .failureHandler(new CustomAuthenticationFailureHandler())
                )
                .headers(headers -> headers.frameOptions().disable())
               //.userDetailsService(userDetailsService) // UserDetailsService 설정
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    }

}
