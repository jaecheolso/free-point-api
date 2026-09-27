package com.freepoint.domain;

import com.freepoint.exception.ErrorCode;
import com.freepoint.exception.PointException;
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
 * 적립 단위. 잔액을 보유하는 유일한 엔티티.
 */
@Entity
@Table(name = "point_lot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointLot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transactionId;

    private Long userId;

    private long originalAmount;

    private long remainingAmount;

    @Enumerated(EnumType.STRING)
    private LotSource source;

    private short usePriority;

    @Enumerated(EnumType.STRING)
    private LotOriginType originType;

    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    private LotStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private PointLot(Long transactionId, Long userId, long amount, LotSource source,
                     LotOriginType originType, LocalDateTime expiresAt, LocalDateTime now) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.originalAmount = amount;
        this.remainingAmount = amount;
        this.source = source;
        this.usePriority = (short) source.usePriority();
        this.originType = originType;
        this.expiresAt = expiresAt;
        this.status = LotStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static PointLot earn(PointTransaction earn, LotSource source, LocalDateTime expiresAt, LocalDateTime now) {
        return new PointLot(earn.getId(), earn.getUserId(), earn.getAmount(), source,
                LotOriginType.EARN, expiresAt, now);
    }

    /**
     * 만료 시각이 되는 순간부터 만료로 본다.
     */
    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    /**
     * 적립취소. 적립 금액 전체가 남아 있는 유효한 일반 적립만 취소할 수 있다.
     */
    public void cancel(LocalDateTime now) {
        if (status == LotStatus.CANCELED) {
            throw new PointException(ErrorCode.ALREADY_CANCELED);
        }
        if (originType == LotOriginType.USE_CANCEL_REISSUE) {
            throw new PointException(ErrorCode.REISSUED_LOT_NOT_CANCELABLE);
        }
        if (isExpired(now)) {
            throw new PointException(ErrorCode.EXPIRED_LOT);
        }
        if (remainingAmount != originalAmount) {
            throw new PointException(ErrorCode.PARTIALLY_USED,
                    "(적립 " + originalAmount + ", 잔여 " + remainingAmount + ")");
        }
        this.remainingAmount = 0;
        this.status = LotStatus.CANCELED;
        this.updatedAt = now;
    }
}
