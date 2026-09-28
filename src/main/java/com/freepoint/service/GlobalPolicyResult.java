package com.freepoint.service;

import com.freepoint.domain.PointPolicy;

import java.time.LocalDateTime;
import java.time.Period;

public record GlobalPolicyResult(
        long minEarnAmount,
        long maxEarnAmount,
        long maxHoldAmount,
        Period minExpirePeriod,
        Period maxExpirePeriod,
        Period defaultExpirePeriod,
        LocalDateTime effectiveFrom
) {

    static GlobalPolicyResult of(PointPolicy policy) {
        return new GlobalPolicyResult(policy.getMinEarnAmount(), policy.getMaxEarnAmount(),
                policy.getMaxHoldAmount(), policy.getMinExpirePeriod(), policy.getMaxExpirePeriod(),
                policy.getDefaultExpirePeriod(), policy.getEffectiveFrom());
    }
}
