package com.freepoint.service;

import com.freepoint.domain.LotSource;

import java.time.LocalDateTime;

/**
 * @param expiresAt null 이면 정책의 기본 만료 기간을 적용한다.
 * @param grantedBy 수기지급(MANUAL)인 경우 필수.
 * @param requestId 클라이언트 요청 멱등 키.
 */
public record EarnCommand(
        Long userId,
        long amount,
        LocalDateTime expiresAt,
        LotSource source,
        String grantedBy,
        String requestId,
        String memo
) {
}
