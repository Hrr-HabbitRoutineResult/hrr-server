package com.hrr.backend.global.logging;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * ERROR 레벨 로그 이벤트를 Discord 웹훅으로 전송하는 Logback appender.
 * webhookUrl이 비어있으면 전송을 완전히 건너뛴다(로컬/테스트 환경 안전).
 * 전송 실패는 전용 내부 logger에도 남기며, DiscordInternalLogFilter로 재귀 전송을 차단한다.
 */
public class DiscordWebhookAppender extends AppenderBase<ILoggingEvent> {

    private static final Logger deliveryLogger = LoggerFactory.getLogger(DiscordInternalLogFilter.INTERNAL_LOGGER_NAME);

    private String webhookUrl;
    private int dedupWindowSeconds = 300;
    private int maxPerMinute = 20;

    private HttpClient httpClient;
    private URI webhookUri;
    private DiscordAlertThrottle throttle;
    private DiscordEmbedPayloadBuilder payloadBuilder;
    private final AtomicInteger consecutiveDeliveryFailures = new AtomicInteger();
    private final AtomicInteger droppedEvents = new AtomicInteger();

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public void setDedupWindowSeconds(int dedupWindowSeconds) {
        this.dedupWindowSeconds = dedupWindowSeconds;
    }

    public void setMaxPerMinute(int maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
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
                        .connectTimeout(Duration.ofSeconds(3))
                        .build();
            } catch (IllegalArgumentException e) {
                // URL 자체에는 Discord webhook token이 포함되므로 설정값을 오류 메시지에 절대 출력하지 않는다.
                addError("Discord webhook URL is invalid; webhook delivery is disabled");
                deliveryLogger.error("[start] Discord webhook URL이 유효하지 않아 알림 전송을 비활성화했습니다.");
            }
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (webhookUri == null) {
            return;
        }
        if (!event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            return;
        }

        long now = System.currentTimeMillis();
        String dedupKey = buildDedupKey(event);

        if (throttle.shouldSuppress(dedupKey, now)) {
            return;
        }
        if (!throttle.tryAcquireGlobalSlot(now)) {
            addWarn("Discord alert rate limit exceeded (maxPerMinute=" + maxPerMinute
                    + "); dropping event from " + event.getLoggerName());
            int droppedCount = droppedEvents.incrementAndGet();
            deliveryLogger.warn("[append] Discord alert rate limit으로 ERROR 이벤트를 폐기했습니다. droppedCount={}, sourceLogger={}",
                    droppedCount, event.getLoggerName());
            if (droppedCount % 20 == 0) {
                deliveryLogger.error("[append] Discord alert rate limit에 의한 이벤트 폐기가 누적되었습니다. droppedCount={}",
                        droppedCount);
            }
            return;
        }

        try {
            String payload = payloadBuilder.buildPayload(event);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(webhookUri)
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 300) {
                addWarn("Discord webhook responded with non-2xx status: " + response.statusCode());
                recordDeliveryFailure("HTTP_" + response.statusCode(), null);
            } else {
                consecutiveDeliveryFailures.set(0);
            }
        } catch (Exception e) {
            addError("Discord webhook POST failed", e);
            recordDeliveryFailure("POST_FAILED", e);
        }
    }

    private void recordDeliveryFailure(String reason, Exception e) {
        int failureCount = consecutiveDeliveryFailures.incrementAndGet();
        deliveryLogger.warn("[recordDeliveryFailure] Discord webhook 전송에 실패했습니다. reason={}, consecutiveFailureCount={}, exception={}",
                reason, failureCount, e != null ? e.getClass().getSimpleName() : "none");
        if (failureCount == 3 || failureCount % 10 == 0) {
            deliveryLogger.error("[recordDeliveryFailure] Discord webhook 연속 전송 실패가 누적되었습니다. reason={}, consecutiveFailureCount={}",
                    reason, failureCount);
        }
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
}
