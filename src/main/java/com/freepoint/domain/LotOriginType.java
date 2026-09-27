package com.freepoint.domain;

/**
 * Lot 발생 경위. 승계되지 않는다.
 */
public enum LotOriginType {
    /** 일반 적립 */
    EARN,
    /** 사용취소 시 원 Lot 이 만료되어 신규 재적립 */
    USE_CANCEL_REISSUE
}
