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

    private PointUseDetail(Long useTransactionId, Long lotId, String orderNo, long amount, int seq,
                           LocalDateTime now) {
        this.useTransactionId = useTransactionId;
        this.lotId = lotId;
        this.orderNo = orderNo;
        this.amount = amount;
        this.canceledAmount = 0;
        this.seq = seq;
        this.createdAt = now;
    }

    public static PointUseDetail of(PointTransaction use, PointLot lot, long amount, int seq, LocalDateTime now) {
        return new PointUseDetail(use.getId(), lot.getId(), use.getOrderNo(), amount, seq, now);
    }

    public long cancelableAmount() {
        return amount - canceledAmount;
    }

    public void cancel(long amount) {
        if (amount <= 0 || amount > cancelableAmount()) {
            throw new IllegalArgumentException(
                    "사용취소 금액이 올바르지 않습니다. useDetailId=" + id + ", amount=" + amount);
        }
        this.canceledAmount += amount;
    }
}
