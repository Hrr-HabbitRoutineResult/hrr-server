package com.hrr.backend.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

class DiscordWebhookAppenderTest {

    private final List<DiscordWebhookAppender> startedAppenders = new ArrayList<>();

    @AfterEach
    void stopAppenders() {
        startedAppenders.forEach(DiscordWebhookAppender::stop);
    }

    @Test
    void buildDedupKey_usesMessageTemplate_insteadOfDynamicArguments() {
        DiscordWebhookAppender appender = new DiscordWebhookAppender();
        ILoggingEvent first = mockEvent("처리 실패. userId={}", "처리 실패. userId=1");
        ILoggingEvent second = mockEvent("처리 실패. userId={}", "처리 실패. userId=999");

        assertThat(appender.buildDedupKey(first)).isEqualTo(appender.buildDedupKey(second));
    }

    @Test
    void start_disablesDeliveryWithoutThrowing_whenWebhookUrlIsInvalid() {
        DiscordWebhookAppender appender = new DiscordWebhookAppender();
        appender.setWebhookUrl("not-an-absolute-url");

        assertThatCode(appender::start).doesNotThrowAnyException();
        startedAppenders.add(appender);
        assertThat(appender.isStarted()).isTrue();
    }

    @Test
    void successfulDelivery_suppressesDuplicateWithinDedupWindow() throws Exception {
        TestAppender appender = startAppender(1);
        appender.enqueueResponse(204);
        ILoggingEvent event = mockEvent("동일 오류. userId={}", "동일 오류. userId=1");

        appender.doAppend(event);
        assertThat(appender.awaitAttempts(1)).isTrue();
        appender.doAppend(mockEvent("동일 오류. userId={}", "동일 오류. userId=2"));

        await(() -> appender.suppressedEventCount() == 1);
        assertThat(appender.sendCount()).isEqualTo(1);
    }

    @Test
    void failedDelivery_releasesDedupReservation_forNextSameError() throws Exception {
        TestAppender appender = startAppender(1);
        appender.enqueueResponse(500);
        ILoggingEvent event = mockEvent("재시도 필요 오류", "재시도 필요 오류");

        appender.doAppend(event);
        await(() -> appender.failedDeliveryEventCount() == 1);

        appender.enqueueResponse(204);
        appender.doAppend(event);

        assertThat(appender.awaitAttempts(2)).isTrue();
        assertThat(appender.sendCount()).isEqualTo(2);
    }

    @Test
    void retryableHttpStatus_retriesAndMarksDelivered_afterSuccess() throws Exception {
        TestAppender appender = startAppender(3);
        appender.enqueueResponse(429, Map.of("Retry-After", List.of("0")));
        appender.enqueueResponse(204);
        ILoggingEvent event = mockEvent("Discord rate limit", "Discord rate limit");

        appender.doAppend(event);

        assertThat(appender.awaitAttempts(2)).isTrue();
        appender.doAppend(event);
        await(() -> appender.suppressedEventCount() == 1);
        assertThat(appender.sendCount()).isEqualTo(2);
        assertThat(appender.failedDeliveryEventCount()).isZero();
    }

    @Test
    void rateLimitRetry_respectsFullRetryAfter_withoutApplyingBackoffCap() throws Exception {
        TestAppender appender = startAppender(2);
        appender.enqueueResponse(429, Map.of("Retry-After", List.of("65")));
        appender.enqueueResponse(204);

        appender.doAppend(mockEvent("긴 Discord rate limit", "긴 Discord rate limit"));

        assertThat(appender.awaitAttempts(2)).isTrue();
        assertThat(appender.lastRetryDelayMillis()).isEqualTo(65_000L);
    }

    @Test
    void networkFailure_retriesAndMarksDelivered_afterSuccess() throws Exception {
        TestAppender appender = startAppender(3);
        appender.enqueueFailure(new IOException("temporary network failure"));
        appender.enqueueResponse(204);

        appender.doAppend(mockEvent("네트워크 오류", "네트워크 오류"));

        assertThat(appender.awaitAttempts(2)).isTrue();
        assertThat(appender.sendCount()).isEqualTo(2);
        assertThat(appender.failedDeliveryEventCount()).isZero();
    }

    @Test
    void retryableFailure_stopsAtMaxAttempts_andReleasesReservation() throws Exception {
        TestAppender appender = startAppender(3);
        appender.enqueueResponse(500);
        appender.enqueueResponse(502);
        appender.enqueueResponse(503);
        ILoggingEvent event = mockEvent("재시도 소진 오류", "재시도 소진 오류");

        appender.doAppend(event);

        await(() -> appender.failedDeliveryEventCount() == 1);
        assertThat(appender.sendCount()).isEqualTo(3);

        appender.enqueueResponse(204);
        appender.doAppend(event);

        assertThat(appender.awaitAttempts(4)).isTrue();
    }

    @Test
    void nonRetryableHttpStatus_doesNotRetry_andReleasesReservation() throws Exception {
        TestAppender appender = startAppender(3);
        appender.enqueueResponse(400);
        ILoggingEvent event = mockEvent("잘못된 웹훅 요청", "잘못된 웹훅 요청");

        appender.doAppend(event);
        await(() -> appender.failedDeliveryEventCount() == 1);

        appender.enqueueResponse(204);
        appender.doAppend(event);

        assertThat(appender.awaitAttempts(2)).isTrue();
        assertThat(appender.sendCount()).isEqualTo(2);
    }

    @Test
    void permanentWebhookFailure_opensCircuit_andRejectsFollowingEvents() throws Exception {
        TestAppender appender = startAppender(3);
        appender.enqueueResponse(404);

        appender.doAppend(mockEvent("삭제된 웹훅", "삭제된 웹훅"));
        await(() -> appender.failedDeliveryEventCount() == 1);

        appender.doAppend(mockEvent("회로 차단 후 오류", "회로 차단 후 오류"));
        await(() -> appender.permanentlyDisabledEventCount() == 1);

        assertThat(appender.sendCount()).isEqualTo(1);
    }

    @Test
    void rateLimit_countsDroppedEvent_withoutAttemptingWebhookDelivery() throws Exception {
        TestAppender appender = new TestAppender();
        appender.setWebhookUrl("https://discord.com/api/webhooks/test/token");
        appender.setMaxAttempts(1);
        appender.setMaxPerMinute(1);
        appender.setMaxFlushTimeMillis(2_000);
        appender.start();
        startedAppenders.add(appender);

        appender.doAppend(mockEvent("첫 번째 알림", "첫 번째 알림"));
        assertThat(appender.awaitAttempts(1)).isTrue();
        appender.doAppend(mockEvent("두 번째 알림", "두 번째 알림"));

        await(() -> appender.rateLimitedEventCount() == 1);
        assertThat(appender.sendCount()).isEqualTo(1);
    }

    @Test
    void queueOverflow_countsDroppedEvent_andReleasesReservation() throws Exception {
        TestAppender appender = new TestAppender();
        appender.setWebhookUrl("https://discord.com/api/webhooks/test/token");
        appender.setMaxAttempts(1);
        appender.setQueueCapacity(1);
        appender.setMaxFlushTimeMillis(2_000);
        appender.blockDelivery();
        appender.start();
        startedAppenders.add(appender);

        appender.doAppend(mockEvent("첫 번째 오류", "첫 번째 오류"));
        assertThat(appender.awaitAttempts(1)).isTrue();
        appender.doAppend(mockEvent("두 번째 오류", "두 번째 오류"));
        ILoggingEvent rejectedEvent = mockEvent("세 번째 오류", "세 번째 오류");
        appender.doAppend(rejectedEvent);

        await(() -> appender.queueDroppedEventCount() == 1);
        appender.unblockDelivery();
        assertThat(appender.awaitAttempts(2)).isTrue();

        appender.doAppend(rejectedEvent);

        assertThat(appender.awaitAttempts(3)).isTrue();
        assertThat(appender.queueDroppedEventCount()).isEqualTo(1);
    }

    private TestAppender startAppender(int maxAttempts) {
        TestAppender appender = new TestAppender();
        appender.setWebhookUrl("https://discord.com/api/webhooks/test/token");
        appender.setMaxAttempts(maxAttempts);
        appender.setRetryBaseDelayMillis(0);
        appender.setMaxRetryDelayMillis(0);
        appender.setMaxFlushTimeMillis(2_000);
        appender.start();
        startedAppenders.add(appender);
        return appender;
    }

    private ILoggingEvent mockEvent(String template, String formattedMessage) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getLoggerName()).thenReturn("test.logger");
        when(event.getMessage()).thenReturn(template);
        when(event.getFormattedMessage()).thenReturn(formattedMessage);
        when(event.getThreadName()).thenReturn("test-thread");
        when(event.getTimeStamp()).thenReturn(1_000_000L);
        when(event.getMDCPropertyMap()).thenReturn(Map.of());
        return event;
    }

    private void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static HttpResponse<Void> response(int statusCode, Map<String, List<String>> headers) {
        @SuppressWarnings("unchecked")
        HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
        return response;
    }

    private static class TestAppender extends DiscordWebhookAppender {

        private final Queue<Object> outcomes = new ConcurrentLinkedQueue<>();
        private final AtomicInteger sendCount = new AtomicInteger();
        private final AtomicLong lastRetryDelayMillis = new AtomicLong(-1L);
        private volatile CountDownLatch deliveryBlock;

        void enqueueResponse(int statusCode) {
            enqueueResponse(statusCode, Map.of());
        }

        void enqueueResponse(int statusCode, Map<String, List<String>> headers) {
            outcomes.add(response(statusCode, headers));
        }

        void enqueueFailure(IOException exception) {
            outcomes.add(exception);
        }

        void blockDelivery() {
            deliveryBlock = new CountDownLatch(1);
        }

        void unblockDelivery() {
            CountDownLatch block = deliveryBlock;
            if (block != null) {
                block.countDown();
            }
        }

        int sendCount() {
            return sendCount.get();
        }

        long lastRetryDelayMillis() {
            return lastRetryDelayMillis.get();
        }

        boolean awaitAttempts(int expectedTotal) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (sendCount.get() < expectedTotal && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            return sendCount.get() >= expectedTotal;
        }

        @Override
        HttpResponse<Void> sendRequest(HttpRequest request) throws IOException, InterruptedException {
            sendCount.incrementAndGet();

            CountDownLatch block = deliveryBlock;
            if (block != null) {
                block.await();
            }

            Object outcome = outcomes.poll();
            if (outcome instanceof IOException exception) {
                throw exception;
            }
            if (outcome instanceof HttpResponse<?> response) {
                @SuppressWarnings("unchecked")
                HttpResponse<Void> typedResponse = (HttpResponse<Void>) response;
                return typedResponse;
            }
            return response(204, Map.of());
        }

        @Override
        void pauseBeforeRetry(long delayMillis) {
            // retry 로직만 검증하고 테스트에서는 실제로 대기하지 않는다.
            lastRetryDelayMillis.set(delayMillis);
        }
    }
}
