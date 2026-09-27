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
     * 사용취소 재적립 Lot. 사용 우선순위 판정 축(source)은 원 Lot 에서 승계한다.
     */
    public static PointLot reissue(PointTransaction reissue, PointLot origin, LocalDateTime expiresAt,
                                   LocalDateTime now) {
        return new PointLot(reissue.getId(), reissue.getUserId(), reissue.getAmount(), origin.source,
                LotOriginType.USE_CANCEL_REISSUE, expiresAt, now);
    }

    public void deduct(long amount, LocalDateTime now) {
        if (status != LotStatus.ACTIVE || isExpired(now) || amount <= 0 || amount > remainingAmount) {
            throw new IllegalStateException(
                    "차감할 수 없는 Lot 입니다. lotId=" + id + ", remaining=" + remainingAmount + ", amount=" + amount);
        }
        this.remainingAmount -= amount;
        this.updatedAt = now;
    }

    /**
     * 사용취소로 원 Lot 잔액을 되돌린다. 만료되지 않은 Lot 에만 호출한다.
     */
    public void restore(long amount, LocalDateTime now) {
        if (status != LotStatus.ACTIVE || isExpired(now) || amount <= 0 || remainingAmount + amount > originalAmount) {
            throw new IllegalStateException(
                    "복원할 수 없는 Lot 입니다. lotId=" + id + ", remaining=" + remainingAmount + ", amount=" + amount);
        }
        this.remainingAmount += amount;
        this.updatedAt = now;
    }

    /**
     * 만료 시각이 되는 순간부터 만료로 본다.
     */
    public boolean isExpired(LocalDateTime now) {
        return !expiresAt.isAfter(now);
    }

    /**
     * 적립취소. 적립 금액 전체가 남아 있는 유효한 적립만 취소할 수 있다.
     * 사용취소 재적립 Lot 도 명세상 "신규적립"이므로 같은 규칙을 적용한다.
     */
    public void cancel(LocalDateTime now) {
        if (status == LotStatus.CANCELED) {
            throw new PointException(ErrorCode.ALREADY_CANCELED);
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
