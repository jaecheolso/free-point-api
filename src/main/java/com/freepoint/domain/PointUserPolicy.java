package com.freepoint.domain;

import com.freepoint.exception.ErrorCode;
import com.freepoint.exception.PointException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 개인별 최대 보유 한도. 보유 한도만 전역 정책을 덮어쓴다.
 * 변경 시 기존 행을 effectiveTo 로 닫고 새 행을 추가하며, 해제 시에는 닫기만 한다.
 */
@Entity
@Table(name = "point_user_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointUserPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private long maxHoldAmount;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private LocalDateTime createdAt;

    public static PointUserPolicy create(Long userId, long maxHoldAmount, LocalDateTime now) {
        if (maxHoldAmount < 1) {
            throw new PointException(ErrorCode.INVALID_POLICY, "(보유 한도 1 이상, 요청 " + maxHoldAmount + ")");
        }
        PointUserPolicy policy = new PointUserPolicy();
        policy.userId = userId;
        policy.maxHoldAmount = maxHoldAmount;
        policy.effectiveFrom = now;
        policy.createdAt = now;
        return policy;
    }

    public void close(LocalDateTime now) {
        this.effectiveTo = now;
    }
}
