package com.freepoint.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 현재 시각은 항상 이 Clock 으로 얻는다. 테스트에서 시각을 조작해 만료 상황을 재현하기 위함.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
