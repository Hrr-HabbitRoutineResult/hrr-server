package com.hrr.backend.global.logging;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * ERROR 레벨 로그 이벤트를 Discord 웹훅으로 비동기 전송하는 Logback appender.
 * webhookUrl이 비어있으면 전송을 완전히 건너뛴다(로컬/테스트 환경 안전).
 * Discord가 2xx로 응답한 이후에만 dedup을 확정하며, 임시 장애는 제한된 횟수로 재시도한다.
 * 전송 실패는 전용 내부 logger에도 남기며, DiscordInternalLogFilter로 재귀 전송을 차단한다.
 */
public class DiscordWebhookAppender extends AppenderBase<ILoggingEvent> {

    private static final Logger deliveryLogger = LoggerFactory.getLogger(DiscordInternalLogFilter.INTERNAL_LOGGER_NAME);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private String webhookUrl;
    private int dedupWindowSeconds = 300;
    private int maxPerMinute = 20;
    private int maxAttempts = 3;
    private long retryBaseDelayMillis = 200;
    private long maxRetryDelayMillis = 2_000;
    private int queueCapacity = 256;
    private long maxFlushTimeMillis = 20_000;

    private HttpClient httpClient;
    private URI webhookUri;
    private DiscordAlertThrottle throttle;
    private DiscordEmbedPayloadBuilder payloadBuilder;
    private ThreadPoolExecutor deliveryExecutor;

    private final AtomicInteger consecutiveDeliveryFailures = new AtomicInteger();
    private final AtomicLong suppressedEvents = new AtomicLong();
    private final AtomicLong rateLimitedEvents = new AtomicLong();
    private final AtomicLong queueDroppedEvents = new AtomicLong();
    private final AtomicLong failedDeliveryEvents = new AtomicLong();
    private final AtomicLong permanentlyDisabledEvents = new AtomicLong();
    private final AtomicReference<String> permanentDisableReason = new AtomicReference<>();

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void setDedupWindowSeconds(int dedupWindowSeconds) {
        this.dedupWindowSeconds = Math.max(0, dedupWindowSeconds);
    }

    public void setMaxPerMinute(int maxPerMinute) {
        this.maxPerMinute = Math.max(1, maxPerMinute);
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    public void setRetryBaseDelayMillis(long retryBaseDelayMillis) {
        this.retryBaseDelayMillis = Math.max(0, retryBaseDelayMillis);
    }

    public void setMaxRetryDelayMillis(long maxRetryDelayMillis) {
        this.maxRetryDelayMillis = Math.max(0, maxRetryDelayMillis);
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = Math.max(1, queueCapacity);
    }

    public void setMaxFlushTimeMillis(long maxFlushTimeMillis) {
        this.maxFlushTimeMillis = Math.max(0, maxFlushTimeMillis);
    }

    @Override
    public void start() {
        this.throttle = new DiscordAlertThrottle(dedupWindowSeconds, maxPerMinute);
        this.payloadBuilder = new DiscordEmbedPayloadBuilder();

        if (webhookUrl != null && !webhookUrl.isBlank()) {
            try {
                URI candidate = URI.create(webhookUrl.trim());
                if (!candidate.isAbsolute()
                        || !"https".equalsIgnoreCase(candidate.getScheme())
                        || candidate.getHost() == null) {
                    throw new IllegalArgumentException("Webhook URL must be an absolute HTTPS URI");
                }
                this.webhookUri = candidate;
                this.httpClient = HttpClient.newBuilder()
                        .connectTimeout(CONNECT_TIMEOUT)
                        .build();
                this.deliveryExecutor = new ThreadPoolExecutor(
                        1,
                        1,
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(queueCapacity),
                        runnable -> {
                            Thread thread = new Thread(runnable, "discord-webhook-delivery");
                            thread.setDaemon(true);
                            return thread;
                        },
                        new ThreadPoolExecutor.AbortPolicy()
                );
            } catch (IllegalArgumentException e) {
                // URL 자체에는 Discord webhook token이 포함되므로 설정값을 오류 메시지에 절대 출력하지 않는다.
                addError("Discord webhook URL is invalid; webhook delivery is disabled");
                deliveryLogger.error("[start] Discord webhook URL이 유효하지 않아 알림 전송을 비활성화했습니다.");
            }
        }
        super.start();
    }

    @Override
    public void stop() {
        super.stop();

        ThreadPoolExecutor executor = this.deliveryExecutor;
        if (executor == null) {
            return;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(maxFlushTimeMillis, TimeUnit.MILLISECONDS)) {
                discardQueuedTasks(executor.shutdownNow(), "shutdown_timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            discardQueuedTasks(executor.shutdownNow(), "shutdown_interrupted");
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (webhookUri == null || deliveryExecutor == null) {
            return;
        }
        if (!event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            return;
        }
        String disabledReason = permanentDisableReason.get();
        if (disabledReason != null) {
            recordPermanentCircuitDrop(event.getLoggerName(), disabledReason);
            return;
        }

        String dedupKey = buildDedupKey(event);
        if (!throttle.tryReserve(dedupKey, System.currentTimeMillis())) {
            suppressedEvents.incrementAndGet();
            return;
        }

        try {
            // MDC, thread name, formatted message를 호출 스레드에서 snapshot으로 고정한다.
            event.prepareForDeferredProcessing();
            deliveryExecutor.execute(new DeliveryTask(event, dedupKey));
        } catch (RejectedExecutionException e) {
            throttle.release(dedupKey);
            recordQueueDrop(event.getLoggerName(), "queue_full");
        } catch (RuntimeException e) {
            throttle.release(dedupKey);
            addError("Discord alert enqueue failed (" + e.getClass().getSimpleName() + ")");
            recordQueueDrop(event.getLoggerName(), "enqueue_failed");
        }
    }

    private void deliver(ILoggingEvent event, String dedupKey) {
        String disabledReason = permanentDisableReason.get();
        if (disabledReason != null) {
            throttle.release(dedupKey);
            recordPermanentCircuitDrop(event.getLoggerName(), disabledReason);
            return;
        }
        if (!throttle.tryAcquireGlobalSlot(System.currentTimeMillis())) {
            throttle.release(dedupKey);
            recordRateLimitDrop(event.getLoggerName());
            return;
        }

        boolean delivered = false;
        try {
            String payload = payloadBuilder.buildPayload(event);
            DeliveryResult result = sendWithRetry(payload);
            if (result.success()) {
                throttle.markDelivered(dedupKey, System.currentTimeMillis());
                consecutiveDeliveryFailures.set(0);
                delivered = true;
            } else {
                recordDeliveryFailure(result.reason(), result.exception());
            }
        } catch (RuntimeException e) {
            addError("Discord webhook payload or request creation failed (" + e.getClass().getSimpleName() + ")");
            recordDeliveryFailure("DELIVERY_PREPARATION_FAILED", e);
        } finally {
            if (!delivered) {
                // 실제 전송이 성공하지 않았으므로 다음 동일 오류가 즉시 다시 시도할 수 있게 한다.
                throttle.release(dedupKey);
            }
        }
    }

    private DeliveryResult sendWithRetry(String payload) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(webhookUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpResponse<Void> response = sendRequest(request);
                int statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return DeliveryResult.succeeded();
                }

                if (!isRetryableStatus(statusCode) || attempt == maxAttempts) {
                    if (isPermanentWebhookFailure(statusCode)) {
                        disableDelivery("HTTP_" + statusCode);
                    }
                    addWarn("Discord webhook responded with non-2xx status: " + statusCode);
                    return DeliveryResult.failed("HTTP_" + statusCode, null);
                }

                pauseBeforeRetry(retryDelayMillis(response, attempt));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return DeliveryResult.failed("INTERRUPTED", e);
            } catch (IOException e) {
                if (attempt == maxAttempts) {
                    // 예외 메시지에 webhook URL/token이 포함될 수 있으므로 예외 유형만 남긴다.
                    addError("Discord webhook POST failed after retries (" + e.getClass().getSimpleName() + ")");
                    return DeliveryResult.failed("POST_FAILED", e);
                }
                try {
                    pauseBeforeRetry(exponentialBackoffMillis(attempt));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return DeliveryResult.failed("INTERRUPTED", interruptedException);
                }
            }
        }

        return DeliveryResult.failed("RETRY_EXHAUSTED", null);
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private boolean isPermanentWebhookFailure(int statusCode) {
        return statusCode == 401 || statusCode == 403 || statusCode == 404;
    }

    private long retryDelayMillis(HttpResponse<Void> response, int attempt) {
        if (response.statusCode() == 429) {
            String retryAfter = response.headers().firstValue("Retry-After")
                    .or(() -> response.headers().firstValue("X-RateLimit-Reset-After"))
                    .orElse(null);
            if (retryAfter != null) {
                try {
                    double seconds = Double.parseDouble(retryAfter);
                    if (Double.isFinite(seconds) && seconds >= 0) {
                        double delayMillis = Math.ceil(seconds * 1_000.0);
                        return delayMillis >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) delayMillis;
                    }
                } catch (NumberFormatException ignored) {
                    // 숫자 형식이 아니면 기본 exponential backoff을 사용한다.
                }
            }
        }
        return exponentialBackoffMillis(attempt);
    }

    private long exponentialBackoffMillis(int attempt) {
        int shift = Math.min(Math.max(0, attempt - 1), 20);
        long multiplier = 1L << shift;
        long delay;
        try {
            delay = Math.multiplyExact(retryBaseDelayMillis, multiplier);
        } catch (ArithmeticException ignored) {
            delay = Long.MAX_VALUE;
        }
        return Math.min(delay, maxRetryDelayMillis);
    }

    HttpResponse<Void> sendRequest(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    void pauseBeforeRetry(long delayMillis) throws InterruptedException {
        if (delayMillis > 0) {
            Thread.sleep(delayMillis);
        }
    }

    private void recordDeliveryFailure(String reason, Exception e) {
        long failedCount = failedDeliveryEvents.incrementAndGet();
        int consecutiveFailureCount = consecutiveDeliveryFailures.incrementAndGet();
        deliveryLogger.warn("[recordDeliveryFailure] Discord webhook 전송을 최종 실패했습니다. reason={}, failedCount={}, consecutiveFailureCount={}, exception={}",
                reason, failedCount, consecutiveFailureCount, e != null ? e.getClass().getSimpleName() : "none");
        if (consecutiveFailureCount == 3 || consecutiveFailureCount % 10 == 0) {
            deliveryLogger.error("[recordDeliveryFailure] Discord webhook 연속 전송 실패가 누적되었습니다. reason={}, failedCount={}, consecutiveFailureCount={}",
                    reason, failedCount, consecutiveFailureCount);
        }
    }

    private void recordRateLimitDrop(String sourceLogger) {
        long droppedCount = rateLimitedEvents.incrementAndGet();
        if (droppedCount == 1 || droppedCount % 20 == 0) {
            deliveryLogger.warn("[recordRateLimitDrop] Discord alert rate limit으로 ERROR 이벤트를 폐기했습니다. rateLimitedCount={}, sourceLogger={}",
                    droppedCount, sourceLogger);
        }
    }

    private void recordQueueDrop(String sourceLogger, String reason) {
        long droppedCount = queueDroppedEvents.incrementAndGet();
        if (droppedCount == 1 || droppedCount % 20 == 0) {
            deliveryLogger.warn("[recordQueueDrop] Discord alert queue에 등록하지 못한 ERROR 이벤트가 누적되었습니다. reason={}, queueDroppedCount={}, sourceLogger={}",
                    reason, droppedCount, sourceLogger);
        }
    }

    private void disableDelivery(String reason) {
        if (permanentDisableReason.compareAndSet(null, reason)) {
            deliveryLogger.error("[disableDelivery] Discord webhook 설정이 영구 실패 응답을 반환해 현재 appender의 전송을 중단합니다. reason={}",
                    reason);
        }
    }

    private void recordPermanentCircuitDrop(String sourceLogger, String reason) {
        long droppedCount = permanentlyDisabledEvents.incrementAndGet();
        if (droppedCount == 1 || droppedCount % 20 == 0) {
            deliveryLogger.warn("[recordPermanentCircuitDrop] 비활성화된 Discord webhook으로 보내지 못한 ERROR 이벤트가 누적되었습니다. reason={}, disabledDropCount={}, sourceLogger={}",
                    reason, droppedCount, sourceLogger);
        }
    }

    private void discardQueuedTasks(List<Runnable> discardedTasks, String reason) {
        if (discardedTasks.isEmpty()) {
            return;
        }

        for (Runnable task : discardedTasks) {
            if (task instanceof DeliveryTask deliveryTask) {
                throttle.release(deliveryTask.dedupKey());
            }
        }

        long droppedCount = queueDroppedEvents.addAndGet(discardedTasks.size());
        deliveryLogger.warn("[discardQueuedTasks] Discord appender 종료 중 queue에 남은 ERROR 이벤트를 폐기했습니다. reason={}, discardedCount={}, queueDroppedCount={}",
                reason, discardedTasks.size(), droppedCount);
    }

    String buildDedupKey(ILoggingEvent event) {
        // 포맷된 메시지에는 userId/challengeId 같은 매번 달라지는 값이 들어갈 수 있으므로,
        // 메시지 템플릿을 기준으로 묶는다. 예외 유형과 최초 발생 위치를 함께 넣어 서로 다른 사고가
        // 한 그룹으로 과도하게 합쳐지는 것은 방지한다.
        String messageTemplate = event.getMessage() == null ? "" : event.getMessage();
        String messagePrefix = messageTemplate.length() > 150
                ? messageTemplate.substring(0, 150)
                : messageTemplate;

        String throwableClass = "";
        String firstFrame = "";
        if (event.getThrowableProxy() != null) {
            throwableClass = event.getThrowableProxy().getClassName();
            var frames = event.getThrowableProxy().getStackTraceElementProxyArray();
            if (frames != null && frames.length > 0) {
                firstFrame = frames[0].getSTEAsString();
            }
        }

        return event.getLoggerName() + "|" + messagePrefix + "|" + throwableClass + "|" + firstFrame;
    }

    long suppressedEventCount() {
        return suppressedEvents.get();
    }

    long rateLimitedEventCount() {
        return rateLimitedEvents.get();
    }

    long queueDroppedEventCount() {
        return queueDroppedEvents.get();
    }

    long failedDeliveryEventCount() {
        return failedDeliveryEvents.get();
    }

    long permanentlyDisabledEventCount() {
        return permanentlyDisabledEvents.get();
    }

    private record DeliveryResult(boolean success, String reason, Exception exception) {

        private static DeliveryResult succeeded() {
            return new DeliveryResult(true, "NONE", null);
        }

        private static DeliveryResult failed(String reason, Exception exception) {
            return new DeliveryResult(false, reason, exception);
        }
    }

    private final class DeliveryTask implements Runnable {

        private final ILoggingEvent event;
        private final String dedupKey;

        private DeliveryTask(ILoggingEvent event, String dedupKey) {
            this.event = event;
            this.dedupKey = dedupKey;
        }

        @Override
        public void run() {
            deliver(event, dedupKey);
        }

        private String dedupKey() {
            return dedupKey;
        }
    }
}
