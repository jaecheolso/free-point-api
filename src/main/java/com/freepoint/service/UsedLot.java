package com.freepoint.service;

/**
 * 사용 금액 중 어느 적립(earnPointKey)에서 얼마를 차감했는지.
 */
public record UsedLot(
        String earnPointKey,
        long amount
) {
}
