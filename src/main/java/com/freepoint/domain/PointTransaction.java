package com.freepoint.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 포인트 거래 원장. pointKey 가 명세 예시의 A, B, C, D, E 에 해당한다.
 */
@Entity
@Table(name = "point_transaction")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String pointKey;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private long amount;

    private long canceledAmount;

    private String orderNo;

    private Long relatedTransactionId;

    private String requestId;

    private String grantedBy;

    private String memo;

    private LocalDateTime createdAt;
}
