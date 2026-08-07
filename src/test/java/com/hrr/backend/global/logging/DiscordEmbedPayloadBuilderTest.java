package com.hrr.backend.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    void buildPayload_는_유효한_JSON_embed를_생성한다() throws Exception {
        ILoggingEvent event = mockEvent("com.hrr.backend.Sample", "테스트 에러 메시지", null);

        String payload = builder.buildPayload(event);

        JsonNode embed = objectMapper.readTree(payload).get("embeds").get(0);
        assertThat(embed.get("title").asText()).isEqualTo("com.hrr.backend.Sample");
        assertThat(embed.get("description").asText()).isEqualTo("테스트 에러 메시지");
        assertThat(embed.get("color").asInt()).isEqualTo(0xE74C3C);
        assertThat(findField(embed.get("fields"), "Thread")).isNotNull();
        assertThat(findField(embed.get("fields"), "Time (Asia/Seoul)")).isNotNull();
    }

    @Test
    void description이_한도를_넘으면_truncate_마커가_붙는다() throws Exception {
        String longMessage = "a".repeat(4000);
        ILoggingEvent event = mockEvent("logger", longMessage, null);

        String payload = builder.buildPayload(event);
        String description = objectMapper.readTree(payload).get("embeds").get(0).get("description").asText();

        assertThat(description.length()).isLessThanOrEqualTo(3500);
        assertThat(description).endsWith("... (truncated)");
    }

    @Test
    void 예외가_없으면_StackTrace_필드가_생성되지_않는다() throws Exception {
        ILoggingEvent event = mockEvent("logger", "에러 발생", null);

        String payload = builder.buildPayload(event);
        JsonNode fields = objectMapper.readTree(payload).get("embeds").get(0).get("fields");

        assertThat(findField(fields, "Stack Trace")).isNull();
    }

    @Test
    void 예외가_있으면_StackTrace_필드가_1024자_이내로_truncate된다() throws Exception {
        RuntimeException deepException = buildDeepException(30);
        IThrowableProxy throwableProxy = new ThrowableProxy(deepException);
        ILoggingEvent event = mockEvent("logger", "에러 발생", throwableProxy);

        String payload = builder.buildPayload(event);
        JsonNode fields = objectMapper.readTree(payload).get("embeds").get(0).get("fields");

        JsonNode stackTraceField = findField(fields, "Stack Trace");
        assertThat(stackTraceField).isNotNull();
        assertThat(stackTraceField.get("value").asText().length()).isLessThanOrEqualTo(1024);
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
        when(event.getFormattedMessage()).thenReturn(message);
        when(event.getThreadName()).thenReturn("test-thread");
        when(event.getTimeStamp()).thenReturn(System.currentTimeMillis());
        when(event.getThrowableProxy()).thenReturn(throwableProxy);
        when(event.getLevel()).thenReturn(Level.ERROR);
        return event;
    }
}
