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

    private PointTransaction(Long userId, TransactionType type, long amount, String orderNo,
                             Long relatedTransactionId, String requestId, String grantedBy,
                             String memo, LocalDateTime now) {
        this.pointKey = PointKeyGenerator.generate();
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.canceledAmount = 0;
        this.orderNo = orderNo;
        this.relatedTransactionId = relatedTransactionId;
        this.requestId = requestId;
        this.grantedBy = grantedBy;
        this.memo = memo;
        this.createdAt = now;
    }

    public static PointTransaction earn(Long userId, long amount, String requestId,
                                        String grantedBy, String memo, LocalDateTime now) {
        return new PointTransaction(userId, TransactionType.EARN, amount, null, null,
                requestId, grantedBy, memo, now);
    }

    /**
     * 사용취소 시 원 Lot 이 만료되어 발생하는 신규 적립. 시스템 생성이므로 requestId 가 없다.
     */
    public static PointTransaction reissue(Long userId, long amount, LocalDateTime now) {
        return new PointTransaction(userId, TransactionType.EARN, amount, null, null,
                null, null, "사용취소 재적립", now);
    }

    public static PointTransaction use(Long userId, long amount, String orderNo, String requestId,
                                       String memo, LocalDateTime now) {
        return new PointTransaction(userId, TransactionType.USE, amount, orderNo, null,
                requestId, null, memo, now);
    }

    public static PointTransaction useCancel(PointTransaction use, long amount, String requestId,
                                             String memo, LocalDateTime now) {
        return new PointTransaction(use.userId, TransactionType.USE_CANCEL, amount, use.orderNo, use.id,
                requestId, null, memo, now);
    }

    public long cancelableAmount() {
        return amount - canceledAmount;
    }

    public static PointTransaction earnCancel(PointTransaction earn, String requestId,
                                              String memo, LocalDateTime now) {
        return new PointTransaction(earn.userId, TransactionType.EARN_CANCEL, earn.amount, null, earn.id,
                requestId, null, memo, now);
    }

    /**
     * 이 거래 금액 중 amount 만큼이 취소되었음을 기록한다.
     */
    public void cancel(long amount) {
        if (amount <= 0 || canceledAmount + amount > this.amount) {
            throw new IllegalArgumentException(
                    "취소 금액이 올바르지 않습니다. amount=" + amount + ", canceled=" + canceledAmount);
        }
        this.canceledAmount += amount;
    }

    public boolean isType(TransactionType type) {
        return this.type == type;
    }
}
