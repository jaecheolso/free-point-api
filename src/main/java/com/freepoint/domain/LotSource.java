package com.freepoint.domain;

/**
 * 적립 출처. 사용 우선순위의 판정 기준이며 사용취소 재적립 시 승계된다.
 * usePriority 가 작을수록 먼저 사용된다.
 */
public enum LotSource {
    MANUAL(1),
    SYSTEM(2);

    private final int usePriority;

    LotSource(int usePriority) {
        this.usePriority = usePriority;
    }

    public int usePriority() {
        return usePriority;
    }
}
