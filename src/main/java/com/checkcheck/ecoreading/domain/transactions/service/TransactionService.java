package com.checkcheck.ecoreading.domain.transactions.service;

import com.checkcheck.ecoreading.domain.boards.entity.Boards;
import com.checkcheck.ecoreading.domain.boards.service.BoardService;
import com.checkcheck.ecoreading.domain.boards.service.BookService;

import com.checkcheck.ecoreading.domain.books.dto.BookMainDTO;
import com.checkcheck.ecoreading.domain.books.entity.Books;
import com.checkcheck.ecoreading.domain.transactions.entity.TransactionStatus;
import com.checkcheck.ecoreading.domain.transactions.entity.Transactions;
import com.checkcheck.ecoreading.domain.transactions.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final BookService bookService;
    private final BoardService boardService;


    public List<Transactions> findAll() {
        return transactionRepository.findAll();
    }
    public List<Boards> findAllByStatus(TransactionStatus statusInput){
        List<Transactions> transactions = transactionRepository.findAll();
        List<Transactions> transactionsSearchByStatus = new ArrayList<>();
        List<Books> books = new ArrayList<>();
        List<Boards> boards = new ArrayList<>();
        for (Transactions t : transactions){
            if (t.getStatus() == statusInput) transactionsSearchByStatus.add(t);
        }
        for (Transactions t : transactionsSearchByStatus){
           books.add(bookService.findBoardByBookId(t.getBooks().getBooksId()));
        }
        for (Books book : books){
            boards.add(boardService.findAllByBoardId(book.getBoards().getBoardId()));
        }
        return boards;
    }

    public Transactions findByBooks(Books books){
        return transactionRepository.findByBooks(books);
    }

    public void updateTransactionStatusFinishCheck(Transactions transactions, Books books){
        // todo: 검수가 끝났고 나눔글이 등록 되었으며, 검수한 책의 등급에 대한 알림
        if (books.getGrade().equals("똥휴지")) {
            transactions.setStatus(TransactionStatus.나눔완료);
            transactionRepository.save(transactions);
        } else {
            transactions.setStatus(TransactionStatus.나눔중);
            transactionRepository.save(transactions);
        }
    }

    public Transactions findByTransactionsId(Long transactionsId){
        return transactionRepository.findByTransactionsId(transactionsId);
    }

    public void updateTransactionsStatus(Transactions transactions){
        // todo: 수거를 시작했다는 알림
        // 매니저가 수거 시작 버튼을 누를 경우에 아래의 코드 동작, 유저는 글 수정 불가, 그리고 유저한테 수거를 시작했다는 알림 전송
        if (transactions.getStatus() == TransactionStatus.신규등록) transactions.setStatus(TransactionStatus.수거중);

        
        else if (transactions.getStatus() == TransactionStatus.수거중 || transactions.getStatus() == TransactionStatus.검수중) {
            transactions.setStatus(TransactionStatus.나눔중);
        }
        transactionRepository.save(transactions);
    }

    public Transactions saveTransaction(Transactions transactions) {
        return transactionRepository.save(transactions);
    }

    // 나눔완료 시 transactions 테이블 값 변경
    public void updateTransactions(BookMainDTO booksDTO, Long id) {
        Transactions transactions = booksDTO.getTransactions();

        transactions.setStatus(TransactionStatus.나눔완료);  // status 나눔완료로 변경
        transactions.setTakerId(id);  // takerId 현재 유저아이디 삽입
        transactions.setSuccessDate(LocalDateTime.now());  // successDate 현재 날짜로 삽입

        saveTransaction(transactions);
    }

    public List<Transactions> findAllByTakerId(Long id){
        return transactionRepository.findByTakerId(id);
    }

}
