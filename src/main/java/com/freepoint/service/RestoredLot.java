package com.freepoint.service;

import com.freepoint.domain.RestoreType;

/**
 * 사용취소 금액이 어느 적립(earnPointKey)으로 얼마나 되돌아갔는지.
 *
 * @param reissuedPointKey 원 적립이 만료되어 신규 적립(REISSUED)된 경우 그 pointKey. 아니면 null.
 */
public record RestoredLot(
        String earnPointKey,
        long amount,
        RestoreType restoreType,
        String reissuedPointKey
) {
}
