package com.checkcheck.ecoreading.domain.boards.controller;

import com.checkcheck.ecoreading.domain.boards.dto.InsertBoardDTO;
import com.checkcheck.ecoreading.domain.boards.dto.InsertBookDTO;
import com.checkcheck.ecoreading.domain.boards.dto.InsertDeliveryDTO;
import com.checkcheck.ecoreading.domain.boards.entity.Boards;
import com.checkcheck.ecoreading.domain.boards.service.BoardService;
import com.checkcheck.ecoreading.domain.boards.service.BookService;
import com.checkcheck.ecoreading.domain.books.dto.BookDTO;
import com.checkcheck.ecoreading.domain.books.dto.BookMainDTO;
import com.checkcheck.ecoreading.domain.books.entity.Books;
import com.checkcheck.ecoreading.domain.delivery.dto.DeliveryDTO;
import com.checkcheck.ecoreading.domain.delivery.entity.Delivery;
import com.checkcheck.ecoreading.domain.delivery.repository.DeliveryRepository;
import com.checkcheck.ecoreading.domain.delivery.service.DeliveryService;
import com.checkcheck.ecoreading.domain.transactions.entity.TransactionStatus;
import com.checkcheck.ecoreading.domain.transactions.entity.Transactions;
import com.checkcheck.ecoreading.domain.transactions.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
@RequestMapping("/board")
public class BoardViewController {

    private final BookService bookService;
    private final BoardService boardService;
    private final DeliveryService deliveryService;
    private final TransactionService transactionService;

    // 나눔글 쓰기
    @GetMapping("/new")
    public String addBoard() {
        return "/content/user/boardAddForm";
    }

    // 나눔 글 상세
    @GetMapping("/detail/{booksId}")
    public String boardDetail(@PathVariable Long booksId, Model model) {
        Books books = bookService.findBoardByBookId(booksId);
        BookMainDTO booksDTO = bookService.convertToDTO(books);  // DTO로 변환

        if (booksDTO == null) {
            return "redirect:/error";
        }

        model.addAttribute("book", booksDTO);

        return "/content/board/boardDetail";
    }

    // 나눔받기
    @GetMapping("/detail/{booksId}/taker")
    public String takeBook(@PathVariable Long booksId, Model model) {
        Books books = bookService.findBoardByBookId(booksId);
        BookMainDTO booksDTO = bookService.convertToDTO(books);  // DTO로 변환
        DeliveryDTO deliveryDTO = deliveryService.convertToDeliveryDTO(new Delivery());

        if (booksDTO == null) {
            return "redirect:/error";
        }

        // 나눔완료시 status=나눔완료, taker_id=현재유저아이디, success_date=현재날짜로 값 삽임
        Transactions transactions = books.getTransactions();
        transactions.setStatus(TransactionStatus.나눔완료);  // status 나눔완료로 변경
        transactions.setTakerId(booksDTO.getBoards().getUsers().getUsersId());  // takerId 현재 유저아이디 삽입
        transactions.setSuccessDate(LocalDateTime.now());  // successDate 현재 날짜로 삽입
        transactionService.saveTransaction(transactions);

        model.addAttribute("book", booksDTO);
        model.addAttribute("delivery", deliveryDTO);

        return "/content/board/takeBook";
    }

    @GetMapping("/update/{boardId}")
    public String boardUpdateForm(@PathVariable Long boardId, Model model){
        Boards boards = boardService.findAllByBoardId(boardId);
        model.addAttribute(boards);
        return "content/board/boardUpdateForm";
    }

    @PostMapping("/update/{boardId}")
    public ModelAndView boardUpdate(@PathVariable Long boardId, @ModelAttribute InsertBookDTO bookDTO, @ModelAttribute InsertBoardDTO boardDTO, @ModelAttribute InsertDeliveryDTO deliveryDTO, Model model, ModelAndView modelAndView){
        boardService.updateBoardByBoardId(boardId, bookDTO, boardDTO, deliveryDTO, model);
        modelAndView.addObject(boardService.updateBoardByBoardId(boardId, bookDTO, boardDTO, deliveryDTO, model));
        modelAndView.setViewName("content/board/updateComplete");
        return modelAndView;
    }

    @GetMapping("/board/update/givelist")
    public String boardList(){
        return "content/mypage/giveList";
    }
}
