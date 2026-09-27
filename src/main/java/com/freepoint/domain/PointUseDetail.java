package com.freepoint.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용 상세. 어떤 사용 거래가 어떤 Lot 에서 얼마를 차감했는지 1원 단위로 기록한다.
 */
@Entity
@Table(name = "point_use_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointUseDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long useTransactionId;

    private Long lotId;

    private String orderNo;

    private long amount;

    private long canceledAmount;

    private int seq;

    private LocalDateTime createdAt;
}
