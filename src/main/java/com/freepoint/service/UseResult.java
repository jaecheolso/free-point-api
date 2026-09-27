package com.freepoint.service;

import java.util.List;

/**
 * @param usedLots 차감된 적립 목록 (사용 순서대로)
 */
public record UseResult(
        String pointKey,
        Long userId,
        String orderNo,
        long amount,
        List<UsedLot> usedLots
) {
}
