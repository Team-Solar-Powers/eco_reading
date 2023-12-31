package com.checkcheck.ecoreading.domain.pointHistory.entity;

import com.checkcheck.ecoreading.domain.BaseEntity;
import com.checkcheck.ecoreading.domain.transactions.entity.Transactions;
import com.checkcheck.ecoreading.domain.users.entity.Users;
import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "point_history")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Getter
@Builder
public class PointHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pointHistoryId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "users_id")
    private Users users;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "transactions_id")
    private Transactions transactions;

    @Enumerated(EnumType.STRING)
    private PointHistoryForm form;
    private int point;

}
