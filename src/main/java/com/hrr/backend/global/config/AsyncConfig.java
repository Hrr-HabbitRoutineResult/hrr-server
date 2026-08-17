package com.hrr.backend.global.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean(name = "getAsyncExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
                int parameterCount = params == null ? 0 : params.length;
                log.error("[getAsyncUncaughtExceptionHandler] 비동기 메서드 실행 중 오류가 발생했습니다. asyncMethod={}, parameterCount={}, eventContext={}",
                        method != null ? method.getName() : "unknown",
                        parameterCount,
                        safeEventContext(params),
                        ex);
        };
    }

    private String safeEventContext(Object[] params) {
        if (params == null || params.length == 0 || params[0] == null) {
            return "none";
        }

        try {
            // 비동기 예외 처리기는 원래 예외를 가리는 2차 예외를 절대 만들면 안 된다.
            // 도메인 객체의 필드나 toString()을 읽지 않고 안전한 클래스명만 남긴다.
            String simpleName = params[0].getClass().getSimpleName();
            return simpleName.isBlank() ? "unknown" : simpleName;
        } catch (RuntimeException ignored) {
            return "unavailable";
        }
    }
}
