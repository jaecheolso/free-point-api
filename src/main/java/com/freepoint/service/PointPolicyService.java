package com.freepoint.service;

import com.freepoint.domain.EffectivePolicy;
import com.freepoint.domain.PointPolicy;
import com.freepoint.domain.PointUserPolicy;
import com.freepoint.exception.ErrorCode;
import com.freepoint.exception.PointException;
import com.freepoint.repository.PointAccountRepository;
import com.freepoint.repository.PointPolicyRepository;
import com.freepoint.repository.PointUserPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointPolicyService {

    private final PointPolicyRepository policyRepository;
    private final PointUserPolicyRepository userPolicyRepository;
    private final PointAccountRepository accountRepository;
    private final Clock clock;

    /**
     * now 시점에 적용되는 정책. 개인별 보유 한도가 있으면 전역 보유 한도를 덮어쓴다.
     */
    public EffectivePolicy getEffectivePolicy(Long userId, LocalDateTime now) {
        PointPolicy global = findGlobalPolicy(now);
        long maxHoldAmount = userPolicyRepository.findEffectiveUserPolicy(userId, now)
                .map(PointUserPolicy::getMaxHoldAmount)
                .orElse(global.getMaxHoldAmount());
        return global.toEffectivePolicy(maxHoldAmount);
    }

    public GlobalPolicyResult getGlobalPolicy() {
        return GlobalPolicyResult.of(findGlobalPolicy(LocalDateTime.now(clock)));
    }

    /**
     * 동시에 변경하면 먼저 커밋된 변경만 반영하고 나머지는 충돌로 거절한다.
     * 뒤의 요청을 이어서 반영하면 앞선 변경을 보지 못한 채 덮어쓰게 되기 때문이다.
     */
    @Transactional
    public GlobalPolicyResult updateGlobalPolicy(long maxEarnAmount, long maxHoldAmount) {
        LocalDateTime now = LocalDateTime.now(clock);
        PointPolicy current = policyRepository.findOpenGlobalPolicyForUpdate()
                .orElseThrow(() -> new PointException(ErrorCode.DATA_CONFLICT));
        PointPolicy next = current.revise(maxEarnAmount, maxHoldAmount, now);
        current.close(now);
        return GlobalPolicyResult.of(policyRepository.save(next));
    }

    public UserPolicyResult getUserPolicy(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!accountRepository.existsById(userId)) {
            throw new PointException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return userPolicyRepository.findEffectiveUserPolicy(userId, now)
                .map(UserPolicyResult::personal)
                .orElseGet(() -> UserPolicyResult.global(userId, findGlobalPolicy(now)));
    }

    /**
     * 적립과 같은 계정 락 하위에서 변경해, 보유 한도 검사 중인 적립과 섞이지 않게 한다.
     */
    @Transactional
    public UserPolicyResult updateUserMaxHold(Long userId, long maxHoldAmount) {
        LocalDateTime now = LocalDateTime.now(clock);
        lockAccount(userId);
        PointUserPolicy next = PointUserPolicy.create(userId, maxHoldAmount, now);
        userPolicyRepository.findEffectiveUserPolicy(userId, now).ifPresent(current -> current.close(now));
        return UserPolicyResult.personal(userPolicyRepository.save(next));
    }

    @Transactional
    public void removeUserPolicy(Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        lockAccount(userId);
        PointUserPolicy current = userPolicyRepository.findEffectiveUserPolicy(userId, now)
                .orElseThrow(() -> new PointException(ErrorCode.USER_POLICY_NOT_FOUND));
        current.close(now);
    }

    private PointPolicy findGlobalPolicy(LocalDateTime now) {
        return policyRepository.findEffectiveGlobalPolicy(now).orElseThrow(() -> noGlobalPolicy(now));
    }

    private IllegalStateException noGlobalPolicy(LocalDateTime now) {
        return new IllegalStateException("적용 가능한 전역 포인트 정책이 없습니다. now=" + now);
    }

    private void lockAccount(Long userId) {
        accountRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new PointException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
