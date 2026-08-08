package com.hrr.backend.global.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import com.hrr.backend.domain.challenge.event.ChallengeCreatedEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeStartEvent;
import com.hrr.backend.domain.notification.event.ChallengeUpdatedEvent;
import com.hrr.backend.domain.notification.event.ChallengeVacancyEvent;
import com.hrr.backend.domain.notification.event.CommentCreatedEvent;
import com.hrr.backend.domain.notification.event.FollowCreatedEvent;
import com.hrr.backend.domain.notification.event.QuestionVerificationCreatedEvent;
import com.hrr.backend.domain.notification.event.WeakVerificationWarningEvent;
import com.hrr.backend.domain.point.event.VerificationPointTriggerEvent;
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
                        method.getName(), parameterCount, safeEventContext(params), ex);
        };
    }

    private String safeEventContext(Object[] params) {
        if (params == null || params.length == 0 || params[0] == null) return "none";
        Object event = params[0];
        if (event instanceof ChallengeCreatedEvent e) return "challengeId=" + e.challengeId();
        if (event instanceof ChallengeExtensionEvent e) return "roundId=" + e.roundId();
        if (event instanceof ChallengeStartEvent e) return "challengeId=" + e.challengeId();
        if (event instanceof ChallengeUpdatedEvent e) return "challengeId=" + e.challengeId();
        if (event instanceof ChallengeVacancyEvent e) return "challengeId=" + e.challengeId();
        if (event instanceof CommentCreatedEvent e) {
            return "verificationId=" + e.verificationId() + ",commentId=" + e.commentId();
        }
        if (event instanceof QuestionVerificationCreatedEvent e) return "verificationId=" + e.verificationId();
        if (event instanceof WeakVerificationWarningEvent e) {
            return "verificationId=" + e.verificationId() + ",warnedUserId=" + e.warnedUserId();
        }
        if (event instanceof FollowCreatedEvent e) {
            return "actorId=" + e.actor().getId() + ",receiverId=" + e.receiver().getId();
        }
        if (event instanceof VerificationPointTriggerEvent e) return "verificationId=" + e.verificationId();
        return event.getClass().getSimpleName();
    }
}
