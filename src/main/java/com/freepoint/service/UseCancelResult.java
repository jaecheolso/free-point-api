package com.freepoint.service;

import java.util.List;

/**
 * @param pointKey                  사용취소 거래의 pointKey.
 * @param usePointKey               원 사용 거래의 pointKey.
 * @param remainingCancelableAmount 원 사용 거래에서 추가로 사용취소할 수 있는 금액.
 */
public record UseCancelResult(
        String pointKey,
        String usePointKey,
        Long userId,
        long amount,
        long remainingCancelableAmount,
        List<RestoredLot> restoredLots
) {
}
