package com.freepoint.service;

import com.freepoint.domain.PointPolicy;
import com.freepoint.repository.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointPolicyService {

    private final PointPolicyRepository policyRepository;

    /**
     * now 시점에 적용되는 정책. 개인별 정책이 있으면 전역 정책보다 우선한다.
     */
    public PointPolicy getEffectivePolicy(Long userId, LocalDateTime now) {
        return policyRepository.findEffectiveUserPolicy(userId, now)
                .or(() -> policyRepository.findEffectiveGlobalPolicy(now))
                .orElseThrow(() -> new IllegalStateException("적용 가능한 전역 포인트 정책이 없습니다. now=" + now));
    }
}
