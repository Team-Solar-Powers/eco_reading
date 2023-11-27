package com.checkcheck.ecoreading.domain.boards.dto;

import com.checkcheck.ecoreading.domain.books.entity.Books;
import com.checkcheck.ecoreading.domain.delivery.entity.Delivery;
import com.checkcheck.ecoreading.domain.users.entity.Users;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminBoardDTO {
	private Long boardId;
	private List<Books> booksList = new ArrayList<>();
	private Users users;
	private List<Delivery> deliveryList = new ArrayList<>();
	private String message;
	private LocalDateTime createdDate;
	private LocalDateTime updateDate;
}
