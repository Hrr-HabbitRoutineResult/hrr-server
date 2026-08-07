package com.hrr.backend.global.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;

/**
 * ERROR 레벨 로그 이벤트를 Discord 웹훅 embed JSON 페이로드로 변환한다.
 * Discord embed 제한(description 4096자, field value 1024자)에 안전 마진을 두고 truncate한다.
 */
public class DiscordEmbedPayloadBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DESCRIPTION_LIMIT = 3500;
    private static final int FIELD_VALUE_LIMIT = 1000;
    private static final int STACK_TRACE_MAX_LINES = 15;
    private static final int RED = 0xE74C3C;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));

    public String buildPayload(ILoggingEvent event) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode embeds = root.putArray("embeds");
        ObjectNode embed = embeds.addObject();

        IThrowableProxy throwableProxy = event.getThrowableProxy();
        String title = throwableProxy != null ? throwableProxy.getClassName() : event.getLoggerName();

        embed.put("title", truncate(title, 256, "..."));
        embed.put("description", truncate(event.getFormattedMessage(), DESCRIPTION_LIMIT, "\n... (truncated)"));
        embed.put("color", RED);

        ArrayNode fields = embed.putArray("fields");
        addField(fields, "Logger", event.getLoggerName());
        addField(fields, "Thread", event.getThreadName());
        addField(fields, "Time (Asia/Seoul)", TIME_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp())));

        if (throwableProxy != null) {
            addField(fields, "Stack Trace", "```\n" + renderStackTrace(throwableProxy) + "\n```");
        }

        return root.toString();
    }

    private void addField(ArrayNode fields, String name, String value) {
        ObjectNode field = fields.addObject();
        field.put("name", name);
        field.put("value", (value == null || value.isBlank()) ? "-" : truncate(value, FIELD_VALUE_LIMIT, "..."));
        field.put("inline", false);
    }

    private String renderStackTrace(IThrowableProxy throwableProxy) {
        String fullTrace = ThrowableProxyUtil.asString(throwableProxy);
        String[] lines = fullTrace.split("\n");

        StringBuilder sb = new StringBuilder();
        int lineCount = Math.min(lines.length, STACK_TRACE_MAX_LINES);
        for (int i = 0; i < lineCount; i++) {
            sb.append(lines[i]).append("\n");
        }
        if (lines.length > STACK_TRACE_MAX_LINES) {
            sb.append("... ").append(lines.length - STACK_TRACE_MAX_LINES).append(" more lines truncated");
        }

        // 코드블록 마크업(````\n`` ``\n````) 여유분을 감안해 필드 한도보다 조금 더 보수적으로 자른다.
        return truncate(sb.toString().stripTrailing(), FIELD_VALUE_LIMIT - 8, "\n... truncated");
    }

    private String truncate(String text, int maxLen, String marker) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        int cut = Math.max(0, maxLen - marker.length());
        return text.substring(0, cut) + marker;
    }
}
