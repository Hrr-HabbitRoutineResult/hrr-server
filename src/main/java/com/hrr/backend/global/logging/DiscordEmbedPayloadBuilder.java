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
 *
 * 지금은 log.error(...)가 찍히는 즉시 건별로 알림을 보내는 "실시간 원본 로그 알림"만 존재한다.
 * 추후 Sentry를 붙이면 이슈 그룹핑, 발생 빈도 요약, 일간/주간 통계 다이제스트 같은
 * 성격이 다른 알림도 같은(혹은 인접한) 채널에 함께 올 수 있으므로, embed 맨 위 author 자리에
 * 알림 종류를 구분할 수 있는 라벨을 남겨둔다. (Discord embed는 author가 title 위, footer가
 * 항상 맨 아래에 고정 렌더링되므로 "맨 위에 보이는 라벨"은 footer가 아니라 author를 써야 한다.)
 */
public class DiscordEmbedPayloadBuilder {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DESCRIPTION_LIMIT = 3500;
    private static final int FIELD_VALUE_LIMIT = 1000;
    private static final int STACK_TRACE_MAX_LINES = 15;
    private static final int RED = 0xE74C3C;
    private static final String AUTHOR_NAME = "Hrr Backend · 실시간 에러 로그 알림";
    // author/title(헤더)과 description(본문)을 시각적으로 분리하는 구분선.
    // Discord embed는 author-title 사이에 여백을 넣을 수 있는 필드가 없어, 대신 이 위치에 넣는다.
    private static final String DIVIDER = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("Asia/Seoul"));

    public String buildPayload(ILoggingEvent event) {
        ObjectNode root = objectMapper.createObjectNode();
        ArrayNode embeds = root.putArray("embeds");
        ObjectNode embed = embeds.addObject();

        // title 위에 뜨는 작은 라벨 (알림 종류 구분용)
        ObjectNode author = embed.putObject("author");
        author.put("name", AUTHOR_NAME);

        IThrowableProxy throwableProxy = event.getThrowableProxy();
        // 채널에 알림이 많이 쌓여도 훑어보기 쉽도록, title은 패키지 없이 클래스 단순명만 사용
        String titleText = throwableProxy != null
                ? simpleName(throwableProxy.getClassName())
                : simpleName(event.getLoggerName());

        embed.put("title", truncate("🚨 " + titleText, 256, "..."));
        String description = DIVIDER + "\n" + truncate(event.getFormattedMessage(), DESCRIPTION_LIMIT, "\n... (truncated)");
        embed.put("description", description);
        embed.put("color", RED);

        // Logger/Thread/Time은 inline(3개)으로 한 줄에 나란히 배치해서 세로 공간을 아낀다
        ArrayNode fields = embed.putArray("fields");
        addField(fields, "Logger", inlineCode(event.getLoggerName()), true);
        addField(fields, "Thread", inlineCode(event.getThreadName()), true);
        addField(fields, "Time (Asia/Seoul)", inlineCode(TIME_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp()))), true);

        if (throwableProxy != null) {
            addField(fields, "Stack Trace", "```\n" + renderStackTrace(throwableProxy) + "\n```", false);
        }

        return root.toString();
    }

    private void addField(ArrayNode fields, String name, String value, boolean inline) {
        ObjectNode field = fields.addObject();
        field.put("name", name);
        field.put("value", (value == null || value.isBlank()) ? "-" : truncate(value, FIELD_VALUE_LIMIT, "..."));
        field.put("inline", inline);
    }

    private String inlineCode(String text) {
        return "`" + (text == null || text.isBlank() ? "-" : text) + "`";
    }

    private String simpleName(String fqcn) {
        if (fqcn == null || fqcn.isBlank()) {
            return "Unknown";
        }
        int lastDot = fqcn.lastIndexOf('.');
        return lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn;
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
