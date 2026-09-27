package com.freepoint.service;

/**
 * @param orderNo   포인트를 사용한 주문번호. 주문 시에만 사용 가능하므로 필수.
 * @param requestId 클라이언트 요청 멱등 키.
 */
public record UseCommand(
        Long userId,
        String orderNo,
        long amount,
        String requestId,
        String memo
) {
}
