package com.freepoint.service;

/**
 * @param pointKey  취소할 적립 거래의 pointKey.
 * @param requestId 클라이언트 요청 멱등 키.
 */
public record EarnCancelCommand(
        String pointKey,
        String requestId,
        String memo
) {
}
