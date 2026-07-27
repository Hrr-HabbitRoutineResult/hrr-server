package com.hrr.backend.global.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시간에 의존하는 로직(RankingScheduler 등)을 테스트에서 고정된 시각으로 검증할 수 있도록
 * 시스템 시계 대신 스프링 빈으로 주입 가능한 Clock을 등록.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}