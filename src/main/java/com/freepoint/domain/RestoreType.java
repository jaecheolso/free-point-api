package com.freepoint.domain;

public enum RestoreType {
    /** 원 Lot 이 유효하여 원 Lot 잔액으로 복원 */
    RESTORED,
    /** 원 Lot 이 만료되어 신규 Lot 으로 재적립 */
    REISSUED
}
