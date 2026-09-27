package com.freepoint.service;

/**
 * @param pointKey  사용취소할 사용 거래의 pointKey.
 * @param amount    사용취소 금액. 사용 금액 중 전체 또는 일부.
 * @param requestId 클라이언트 요청 멱등 키.
 */
public record UseCancelCommand(
        String pointKey,
        long amount,
        String requestId,
        String memo
) {
}
