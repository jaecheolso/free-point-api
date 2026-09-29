package com.freepoint.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 현재 시각은 항상 이 Clock 으로 얻는다. 테스트에서 시각을 조작해 만료 상황을 재현하기 위함.
 * 서버 기본 시간대(컨테이너는 보통 UTC)와 무관하게 KST 로 고정한다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
