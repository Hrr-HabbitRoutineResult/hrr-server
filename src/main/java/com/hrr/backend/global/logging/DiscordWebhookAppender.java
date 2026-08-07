package com.hrr.backend.global.logging;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * ERROR 레벨 로그 이벤트를 Discord 웹훅으로 전송하는 Logback appender.
 * webhookUrl이 비어있으면 전송을 완전히 건너뛴다(로컬/테스트 환경 안전).
 * 전송 실패는 절대 log.error(...)로 재귀 보고하지 않고 Logback StatusManager(addError/addWarn)로만 남긴다.
 */
public class DiscordWebhookAppender extends AppenderBase<ILoggingEvent> {

    private String webhookUrl;
    private int dedupWindowSeconds = 300;
    private int maxPerMinute = 20;

    private HttpClient httpClient;
    private DiscordAlertThrottle throttle;
    private DiscordEmbedPayloadBuilder payloadBuilder;

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
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.throttle = new DiscordAlertThrottle(dedupWindowSeconds, maxPerMinute);
        this.payloadBuilder = new DiscordEmbedPayloadBuilder();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
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
            return;
        }

        try {
            String payload = payloadBuilder.buildPayload(event);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 300) {
                addWarn("Discord webhook responded with non-2xx status: " + response.statusCode());
            }
        } catch (Exception e) {
            addError("Discord webhook POST failed", e);
        }
    }

    private String buildDedupKey(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        String messagePrefix = message.length() > 100 ? message.substring(0, 100) : message;
        return event.getLoggerName() + "|" + messagePrefix;
    }
}
