package com.hrr.backend.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

class AsyncConfigTest {

    @Test
    void asyncExceptionHandler_doesNotThrow_whenMethodOrEventContextIsMissing() {
        AsyncUncaughtExceptionHandler handler = new AsyncConfig().getAsyncUncaughtExceptionHandler();

        assertThatCode(() -> handler.handleUncaughtException(
                new RuntimeException("original async failure"),
                null,
                new Object[] {null}
        )).doesNotThrowAnyException();
    }

    @Test
    void asyncExceptionHandler_doesNotReadDomainFieldsOrToString() {
        AsyncUncaughtExceptionHandler handler = new AsyncConfig().getAsyncUncaughtExceptionHandler();
        Object unsafeEvent = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("must not call toString");
            }
        };

        assertThatCode(() -> handler.handleUncaughtException(
                new RuntimeException("original async failure"),
                null,
                unsafeEvent
        )).doesNotThrowAnyException();
    }
}
