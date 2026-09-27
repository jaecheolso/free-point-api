package com.freepoint.domain;

import com.github.f4b6a3.uuid.UuidCreator;

/**
 * pointKey 생성기. UUID v7 (RFC 9562) 을 사용한다.
 * 앞 48비트가 밀리초 시각이라 생성 순서대로 정렬되어 point_key 인덱스에 순차 삽입된다.
 * 같은 밀리초 안에서도 순서를 보장하도록 단조 증가 방식(Plus1)을 쓴다.
 */
public final class PointKeyGenerator {

    private PointKeyGenerator() {
    }

    public static String generate() {
        return UuidCreator.getTimeOrderedEpochPlus1().toString();
    }
}
