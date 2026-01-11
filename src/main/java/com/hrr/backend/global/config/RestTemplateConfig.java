package com.hrr.backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

		// 연결 제한 시간: 10초 (네트워크 연결 시도 시간)
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());

		// 읽기 제한 시간: 60초 (모델 로딩 및 연산 대기 시간)
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());

        return builder
                .requestFactory(() -> factory)
                .build();
    }
}
