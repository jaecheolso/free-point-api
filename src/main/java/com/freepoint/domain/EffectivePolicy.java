package com.freepoint.domain;

import com.freepoint.exception.ErrorCode;
import com.freepoint.exception.PointException;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * 특정 사용자에게 실제로 적용되는 정책. 전역 정책에 개인별 보유 한도를 덮어쓴 값이다.
 */
public record EffectivePolicy(
        long minEarnAmount,
        long maxEarnAmount,
        long maxHoldAmount,
        Period minExpirePeriod,
        Period maxExpirePeriod,
        Period defaultExpirePeriod
) {

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
        // DB(TIMESTAMP)의 소수점 이하 초 정밀도에 따라 저장 값이 달라지지 않도록 초 단위로 맞춘다.
        LocalDateTime normalized = requested.truncatedTo(ChronoUnit.SECONDS);
        LocalDateTime min = now.plus(minExpirePeriod);
        LocalDateTime max = now.plus(maxExpirePeriod);
        if (normalized.isBefore(min) || !normalized.isBefore(max)) {
            throw new PointException(ErrorCode.INVALID_EXPIRES_AT,
                    "(허용 범위 " + min + " 이상 " + max + " 미만, 요청 " + requested + ")");
        }
        return normalized;
    }
}
