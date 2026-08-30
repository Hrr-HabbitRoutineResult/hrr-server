package com.hrr.backend.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;

class DiscordEmbedPayloadBuilderTest {

    private final DiscordEmbedPayloadBuilder builder = new DiscordEmbedPayloadBuilder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void buildPayload_generatesValidJsonEmbed() throws Exception {
        ILoggingEvent event = mockEvent("com.hrr.backend.Sample", "테스트 에러 메시지", null);

        String payload = builder.buildPayload(event);

        JsonNode embed = objectMapper.readTree(payload).get("embeds").get(0);
        // 패키지 없이 클래스 단순명만 title에 노출
        assertThat(embed.get("title").asText()).isEqualTo("🚨 에러 발생 비이이이이상 [Sample]");
        // description 맨 위엔 author/title(헤더)과 본문을 나누는 구분선이 붙고, 그 아래에 실제 메시지가 온다
        assertThat(embed.get("description").asText()).endsWith("\n테스트 에러 메시지");
        assertThat(embed.get("color").asInt()).isEqualTo(0xE74C3C);
        assertThat(embed.get("author").get("name").asText()).isEqualTo("Hrr Backend · 실시간 에러 로그 알림");
        // 필드 순서: 요청 Handler -> 발생 시간 -> Logger -> Thread
        assertThat(embed.get("fields").get(0).get("name").asText()).isEqualTo("🌐 요청 Handler");
        assertThat(embed.get("fields").get(1).get("name").asText()).isEqualTo("⏰ 발생 시간");
        assertThat(embed.get("fields").get(2).get("name").asText()).isEqualTo("Logger");
        assertThat(embed.get("fields").get(3).get("name").asText()).isEqualTo("Thread");
        assertThat(findField(embed.get("fields"), "Logger").get("value").asText()).isEqualTo("`com.hrr.backend.Sample`");
        assertThat(findField(embed.get("fields"), "Thread")).isNotNull();
        assertThat(findField(embed.get("fields"), "Thread").get("inline").asBoolean()).isTrue();
        assertThat(findField(embed.get("fields"), "⏰ 발생 시간")).isNotNull();
    }

    @Test
    void requestHandlerField_showsMdcValue_whenPresent() throws Exception {
        ILoggingEvent event = mockEvent("logger", "에러 발생", null);
        when(event.getMDCPropertyMap())
                .thenReturn(Map.of(RequestContextLoggingFilter.MDC_KEY_HANDLER, "GET TestController.discordAlertTest"));

        String payload = builder.buildPayload(event);
        JsonNode field = findField(objectMapper.readTree(payload).get("embeds").get(0).get("fields"), "🌐 요청 Handler");

        assertThat(field).isNotNull();
        assertThat(field.get("value").asText()).isEqualTo("`GET TestController.discordAlertTest`");
    }

    @Test
    void requestHandlerField_showsFallbackText_whenMdcMissing() throws Exception {
        // 스케줄러/비동기 리스너처럼 HTTP 요청 컨텍스트가 없는 경우를 가정 (mockEvent 기본값 = 빈 MDC)
        ILoggingEvent event = mockEvent("logger", "에러 발생", null);

        String payload = builder.buildPayload(event);
        JsonNode field = findField(objectMapper.readTree(payload).get("embeds").get(0).get("fields"), "🌐 요청 Handler");

        assertThat(field).isNotNull();
        assertThat(field.get("value").asText()).contains("스케줄러/비동기");
    }

    @Test
    void requestHandlerField_escapesMarkdownAndTruncatesLongValue() throws Exception {
        ILoggingEvent event = mockEvent("logger", "에러 발생", null);
        when(event.getMDCPropertyMap()).thenReturn(Map.of(
                RequestContextLoggingFilter.MDC_KEY_HANDLER,
                "GET `unsafe`" + "a".repeat(700) + "\nnext-line"));

        String payload = builder.buildPayload(event);
        JsonNode field = findField(objectMapper.readTree(payload).get("embeds").get(0).get("fields"), "🌐 요청 Handler");

        assertThat(field.get("value").asText())
                .doesNotContain("`unsafe`")
                .doesNotContain("\n")
                .hasSizeLessThanOrEqualTo(300);
    }

    @Test
    void description_fallsBackToExceptionClass_whenLogMessageIsGenericAdviceBoilerplate() throws Exception {
        // ExceptionAdvice.exception()이 남기는 고정 문구를 그대로 흉내
        RuntimeException exception = new RuntimeException("실제로 발생한 예외 메시지");
        IThrowableProxy throwableProxy = new ThrowableProxy(exception);
        ILoggingEvent event = mockEvent("logger", "[exception] 처리하지 못한 예외가 발생했습니다.", throwableProxy);

        String payload = builder.buildPayload(event);
        String description = objectMapper.readTree(payload).get("embeds").get(0).get("description").asText();

        assertThat(description).endsWith("\nRuntimeException").doesNotContain("실제로 발생한 예외 메시지");
    }

    @Test
    void description_keepsOriginalMessage_whenNotGenericAdviceBoilerplate() throws Exception {
        ILoggingEvent event = mockEvent("logger", "[socialLogin] Kakao 로그인 중 오류가 발생했습니다.", null);

        String payload = builder.buildPayload(event);
        String description = objectMapper.readTree(payload).get("embeds").get(0).get("description").asText();

        assertThat(description).endsWith("\n[socialLogin] Kakao 로그인 중 오류가 발생했습니다.");
    }

    @Test
    void description_masksSensitiveValues() throws Exception {
        String message = "client_secret=secret-value&access_token=token-value "
                + "email=user@example.com&nickname=private-name Authorization: Bearer bearer-value "
                + "https://discord.com/api/webhooks/123/webhook-token";
        ILoggingEvent event = mockEvent("logger", message, null);

        String payload = builder.buildPayload(event);
        String description = objectMapper.readTree(payload).get("embeds").get(0).get("description").asText();

        assertThat(description)
                .contains("***MASKED***")
                .doesNotContain("secret-value", "token-value", "user@example.com", "private-name", "bearer-value",
                        "webhook-token");
    }

    @Test
    void stackTraceField_excludesExceptionMessageContainingSecrets() throws Exception {
        RuntimeException exception = new RuntimeException(
                "GET https://example.com?refresh_token=refresh-value&code=authorization-code");
        ILoggingEvent event = mockEvent("logger", "[request] 외부 API 호출에 실패했습니다.", new ThrowableProxy(exception));

        String payload = builder.buildPayload(event);
        String stackTrace = findField(
                objectMapper.readTree(payload).get("embeds").get(0).get("fields"), "📜 Stack Trace")
                .get("value").asText();

        assertThat(stackTrace)
                .contains("RuntimeException")
                .doesNotContain("refresh-value", "authorization-code", "example.com");
    }

    @Test
    void description_truncatesWithMarker_whenOverLimit() throws Exception {
        String longMessage = "a".repeat(4000);
        ILoggingEvent event = mockEvent("logger", longMessage, null);

        String payload = builder.buildPayload(event);
        String description = objectMapper.readTree(payload).get("embeds").get(0).get("description").asText();

        // Discord embed 전체 6000자 제한에 여유를 남기도록 description 예산을 제한한다.
        assertThat(description.length()).isLessThanOrEqualTo(3000 + 50);
        assertThat(description).endsWith("... (truncated)");
    }

    @Test
    void description_masksSecretsAndBoundsVeryLargeInput() throws Exception {
        String longMessage = "access_token=secret-value " + "a".repeat(50_000);
        ILoggingEvent event = mockEvent("logger", longMessage, null);

        String payload = builder.buildPayload(event);
        String description = objectMapper.readTree(payload).get("embeds").get(0).get("description").asText();

        assertThat(description)
                .contains("***MASKED***")
                .doesNotContain("secret-value")
                .endsWith("... (truncated)");
    }

    @Test
    void stackTraceField_isAbsent_whenNoException() throws Exception {
        ILoggingEvent event = mockEvent("logger", "에러 발생", null);

        String payload = builder.buildPayload(event);
        JsonNode fields = objectMapper.readTree(payload).get("embeds").get(0).get("fields");

        assertThat(findField(fields, "📜 Stack Trace")).isNull();
    }

    @Test
    void stackTraceField_truncatesWithin1024Chars_whenExceptionPresent() throws Exception {
        RuntimeException deepException = buildDeepException(30);
        IThrowableProxy throwableProxy = new ThrowableProxy(deepException);
        ILoggingEvent event = mockEvent("logger", "에러 발생", throwableProxy);

        String payload = builder.buildPayload(event);
        JsonNode embed = objectMapper.readTree(payload).get("embeds").get(0);

        // 예외가 있을 때는 title이 로거명이 아니라 예외 클래스 단순명으로 뜬다
        assertThat(embed.get("title").asText()).isEqualTo("🚨 에러 발생 비이이이이상 [RuntimeException]");

        JsonNode stackTraceField = findField(embed.get("fields"), "📜 Stack Trace");
        assertThat(stackTraceField).isNotNull();
        assertThat(stackTraceField.get("inline").asBoolean()).isFalse();
        assertThat(stackTraceField.get("value").asText().length()).isLessThanOrEqualTo(1024);
    }

    @Test
    void stackTraceField_escapesBackticks_thatCouldCloseCodeBlock() throws Exception {
        IThrowableProxy throwableProxy = new ThrowableProxy(new RuntimeException("```unsafe```"));
        ILoggingEvent event = mockEvent("logger", "에러 발생", throwableProxy);

        String payload = builder.buildPayload(event);
        JsonNode stackTraceField = findField(
                objectMapper.readTree(payload).get("embeds").get(0).get("fields"), "📜 Stack Trace");

        assertThat(stackTraceField.get("value").asText()).doesNotContain("```unsafe```");
    }

    private RuntimeException buildDeepException(int stackDepth) {
        try {
            recurse(stackDepth);
            throw new IllegalStateException("unreachable");
        } catch (RuntimeException e) {
            return e;
        }
    }

    private void recurse(int depth) {
        if (depth <= 0) {
            throw new RuntimeException("boom");
        }
        recurse(depth - 1);
    }

    private JsonNode findField(JsonNode fields, String name) {
        for (JsonNode field : fields) {
            if (field.get("name").asText().equals(name)) {
                return field;
            }
        }
        return null;
    }

    private ILoggingEvent mockEvent(String loggerName, String message, IThrowableProxy throwableProxy) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getLoggerName()).thenReturn(loggerName);
        when(event.getMessage()).thenReturn(message);
        when(event.getFormattedMessage()).thenReturn(message);
        when(event.getThreadName()).thenReturn("test-thread");
        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getThrowableProxy()).thenReturn(throwableProxy);
        when(event.getLevel()).thenReturn(Level.ERROR);
        when(event.getMDCPropertyMap()).thenReturn(Collections.emptyMap());
        return event;
    }
}
