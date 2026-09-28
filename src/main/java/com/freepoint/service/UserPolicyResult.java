package com.freepoint.service;

import com.freepoint.domain.PointPolicy;
import com.freepoint.domain.PointUserPolicy;

import java.time.LocalDateTime;

/**
 * @param personal 개인별 보유 한도 적용 여부. false 면 전역 보유 한도를 따른다.
 */
public record UserPolicyResult(
        Long userId,
        long maxHoldAmount,
        boolean personal,
        LocalDateTime effectiveFrom
) {

    static UserPolicyResult personal(PointUserPolicy policy) {
        return new UserPolicyResult(policy.getUserId(), policy.getMaxHoldAmount(), true, policy.getEffectiveFrom());
    }

    static UserPolicyResult global(Long userId, PointPolicy policy) {
        return new UserPolicyResult(userId, policy.getMaxHoldAmount(), false, policy.getEffectiveFrom());
    }
}
