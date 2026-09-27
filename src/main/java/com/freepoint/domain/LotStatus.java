package com.freepoint.domain;

/**
 * 만료는 상태로 저장하지 않고 expiresAt 으로 판정한다.
 */
public enum LotStatus {
    ACTIVE,
    CANCELED
}
