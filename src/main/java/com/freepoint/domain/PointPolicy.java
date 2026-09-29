package com.freepoint.domain;

import com.freepoint.exception.ErrorCode;
import com.freepoint.exception.PointException;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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
 * 전역 포인트 정책. 변경 시 기존 행을 수정하지 않고 effectiveTo 로 닫은 뒤 새 행을 추가한다.
 */
@Entity
@Table(name = "point_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    /**
     * 1회 최대 적립액과 보유 한도를 바꾼 다음 정책을 만든다. 나머지 항목은 명세 고정값이므로 그대로 승계한다.
     */
    public PointPolicy revise(long maxEarnAmount, long maxHoldAmount, LocalDateTime now) {
        if (maxEarnAmount < minEarnAmount || maxHoldAmount < 1) {
            throw new PointException(ErrorCode.INVALID_POLICY,
                    "(1회 최대 적립액 " + minEarnAmount + " 이상, 보유 한도 1 이상, 요청 "
                            + maxEarnAmount + " / " + maxHoldAmount + ")");
        }
        PointPolicy next = new PointPolicy();
        next.minEarnAmount = minEarnAmount;
        next.maxEarnAmount = maxEarnAmount;
        next.maxHoldAmount = maxHoldAmount;
        next.minExpirePeriod = minExpirePeriod;
        next.maxExpirePeriod = maxExpirePeriod;
        next.defaultExpirePeriod = defaultExpirePeriod;
        next.effectiveFrom = now;
        next.createdAt = now;
        return next;
    }

    public EffectivePolicy toEffectivePolicy(long maxHoldAmount) {
        return new EffectivePolicy(minEarnAmount, maxEarnAmount, maxHoldAmount,
                minExpirePeriod, maxExpirePeriod, defaultExpirePeriod);
    }
}
