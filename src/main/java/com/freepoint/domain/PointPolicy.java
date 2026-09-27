package com.freepoint.domain;

import com.freepoint.exception.ErrorCode;
import com.freepoint.exception.PointException;
import jakarta.persistence.Convert;
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
import java.time.Period;

/**
 * 포인트 정책. 변경 시 기존 행을 수정하지 않고 effectiveTo 로 닫은 뒤 새 행을 추가한다.
 */
@Entity
@Table(name = "point_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PolicyScope scope;

    private Long userId;

    private long minEarnAmount;

    private long maxEarnAmount;

    private long maxHoldAmount;

    @Convert(converter = PeriodConverter.class)
    private Period minExpirePeriod;

    @Convert(converter = PeriodConverter.class)
    private Period maxExpirePeriod;

    @Convert(converter = PeriodConverter.class)
    private Period defaultExpirePeriod;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private LocalDateTime createdAt;

    public void validateEarnAmount(long amount) {
        if (amount < minEarnAmount || amount > maxEarnAmount) {
            throw new PointException(ErrorCode.INVALID_EARN_AMOUNT,
                    "(허용 범위 " + minEarnAmount + " ~ " + maxEarnAmount + ", 요청 " + amount + ")");
        }
    }

    public void validateHoldLimit(long currentBalance, long earnAmount) {
        if (currentBalance + earnAmount > maxHoldAmount) {
            throw new PointException(ErrorCode.HOLD_LIMIT_EXCEEDED,
                    "(최대 " + maxHoldAmount + ", 현재 " + currentBalance + ", 요청 " + earnAmount + ")");
        }
    }

    /**
     * 요청 만료일을 검증해 확정한다. 지정하지 않으면 기본 만료 기간을 적용한다.
     * 허용 범위: now + 최소기간 <= expiresAt < now + 최대기간 (명세 "최대 5년 미만")
     */
    public LocalDateTime resolveExpiresAt(LocalDateTime requested, LocalDateTime now) {
        if (requested == null) {
            return now.plus(defaultExpirePeriod);
        }
        LocalDateTime min = now.plus(minExpirePeriod);
        LocalDateTime max = now.plus(maxExpirePeriod);
        if (requested.isBefore(min) || !requested.isBefore(max)) {
            throw new PointException(ErrorCode.INVALID_EXPIRES_AT,
                    "(허용 범위 " + min + " 이상 " + max + " 미만, 요청 " + requested + ")");
        }
        return requested;
    }
}
