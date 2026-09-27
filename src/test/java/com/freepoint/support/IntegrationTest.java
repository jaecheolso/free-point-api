package com.freepoint.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 서비스 통합 테스트 공통 설정.
 * - 테스트마다 트랜잭션을 롤백해 data.sql 초기 상태(전역 정책 1건, 계정 1~5)에서 시작한다.
 * - 시각은 MutableClock 으로 고정하고 필요 시 앞으로 돌린다.
 */
@SpringBootTest
@Transactional
@Import(IntegrationTest.ClockConfig.class)
public abstract class IntegrationTest {

    protected static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    protected static final Instant BASE_INSTANT = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    protected MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.setInstant(BASE_INSTANT);
    }

    protected LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    @TestConfiguration
    static class ClockConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(BASE_INSTANT, ZONE);
        }
    }
}
