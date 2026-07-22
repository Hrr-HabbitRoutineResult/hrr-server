package com.hrr.backend.global.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

		// 연결 제한 시간: 10초 (네트워크 연결 시도 시간)
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());

		// 읽기 제한 시간: 60초 (카카오/네이버/애플 등 일반 외부 API 응답 대기 시간)
        factory.setReadTimeout((int) Duration.ofSeconds(60).toMillis());

        return builder
                .requestFactory(() -> factory)
                .build();
    }

    // 모델(추천/임베딩) 서버 전용 RestTemplate.
    // Lambda 콜드스타트 시의 모델 로딩 대기 시간을 고려하여, 기본 RestTemplate과 분리해 넉넉한 타임아웃을 둔다.
    @Bean
    @Qualifier("modelApiRestTemplate")
    public RestTemplate modelApiRestTemplate(RestTemplateBuilder builder) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 연결 제한 시간: 10초 (네트워크 연결 시도 시간)
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());

        // 읽기 제한 시간: 200초 (Lambda 콜드스타트 시 모델 로딩 대기 시간 + 여유)
        factory.setReadTimeout((int) Duration.ofSeconds(200).toMillis());

        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
