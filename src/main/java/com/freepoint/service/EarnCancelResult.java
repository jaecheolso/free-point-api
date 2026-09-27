package com.freepoint.service;

import com.freepoint.domain.PointTransaction;

/**
 * @param pointKey     적립취소 거래의 pointKey.
 * @param earnPointKey 취소된 원 적립 거래의 pointKey.
 */
public record EarnCancelResult(
        String pointKey,
        String earnPointKey,
        Long userId,
        long amount
) {

    static EarnCancelResult of(PointTransaction cancel, PointTransaction earn) {
        return new EarnCancelResult(cancel.getPointKey(), earn.getPointKey(), cancel.getUserId(), cancel.getAmount());
    }
}
