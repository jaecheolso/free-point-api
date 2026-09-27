package com.freepoint.service;

import com.freepoint.domain.LotSource;
import com.freepoint.domain.PointLot;
import com.freepoint.domain.PointTransaction;

import java.time.LocalDateTime;

public record EarnResult(
        String pointKey,
        Long userId,
        long amount,
        LotSource source,
        LocalDateTime expiresAt
) {

    static EarnResult of(PointTransaction earn, PointLot lot) {
        return new EarnResult(earn.getPointKey(), earn.getUserId(), earn.getAmount(),
                lot.getSource(), lot.getExpiresAt());
    }
}
