package com.hrr.backend.global.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

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

    // Discord는 embed 하나의 모든 텍스트 합계를 6000자로 제한한다. 각 필드의 개별 제한뿐 아니라
    // 전체 제한에도 여유를 남기기 위해 description/API/logger/thread 예산을 따로 둔다.
    private static final int DESCRIPTION_LIMIT = 3000;
    private static final int RAW_MESSAGE_INPUT_LIMIT = 12_000;
    private static final int FIELD_VALUE_LIMIT = 1000;
    private static final int HANDLER_VALUE_LIMIT = 300;
    private static final int LOGGER_VALUE_LIMIT = 350;
    private static final int THREAD_VALUE_LIMIT = 200;
    private static final int STACK_TRACE_MAX_LINES = 15;
    private static final int RED = 0xE74C3C;
    private static final String AUTHOR_NAME = "Hrr Backend · 실시간 에러 로그 알림";
    // author/title(헤더)과 description(본문)을 시각적으로 분리하는 구분선.
    // Discord embed는 author-title 사이에 여백을 넣을 수 있는 필드가 없어, 대신 이 위치에 넣는다.
    private static final String DIVIDER = "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬";
    // ExceptionAdvice.exception()의 고정 문구 - 던져진 예외 자체에 대한 정보가 없어 그대로 보여줘도 가치가 없다
    private static final String GENERIC_UNHANDLED_MESSAGE = "[exception] 처리하지 못한 예외가 발생했습니다.";
    // 요청 컨텍스트(MDC)가 없는 경우(스케줄러/비동기 등)에 요청 Handler 필드에 대신 표시할 안내 문구
    private static final String NO_API_CONTEXT = "없음 (스케줄러/비동기 등, HTTP 요청 아님)";
    private static final String MASK = "***MASKED***";

    private static final Pattern SENSITIVE_HEADER = Pattern.compile(
            "(?i)(authorization|proxy-authorization|cookie|set-cookie)\\s*[:=]\\s*[^\\r\\n]+"
    );
    private static final Pattern LABELED_SECRET = Pattern.compile(
            "(?i)(?<![A-Za-z0-9_])"
                    + "(access[_-]?token|refresh[_-]?token|id[_-]?token|token|client[_-]?secret|secret|"
                    + "password|passwd|private[_-]?key|code|email|display[_-]?name|nickname|name|"
                    + "phone(?:[_-]?number)?|address)"
                    + "\\s*[:=]\\s*([^\\s&,}\\]]+)"
    );
    private static final Pattern JSON_SECRET = Pattern.compile(
            "(?i)([\"'](?:access[_-]?token|refresh[_-]?token|id[_-]?token|token|client[_-]?secret|secret|"
                    + "password|passwd|private[_-]?key|code|email|display[_-]?name|nickname|name|"
                    + "phone(?:[_-]?number)?|address)[\"']\\s*:\\s*[\"'])[^\"']*([\"'])"
    );
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/-]+=*");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b");
    private static final Pattern EMAIL = Pattern.compile(
            "(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b"
    );
    private static final Pattern DISCORD_WEBHOOK = Pattern.compile(
            "(?i)(https://(?:canary\\.|ptb\\.)?discord(?:app)?\\.com/api/webhooks/)[^\\s]+"
    );
    private static final Pattern PRIVATE_KEY_BLOCK = Pattern.compile(
            "(?s)-----BEGIN [^-]*PRIVATE KEY-----.*?-----END [^-]*PRIVATE KEY-----"
    );

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

        embed.put("title", truncate("🚨 에러 발생 [" + titleText + "]", 256, "..."));

        // ExceptionAdvice의 catch-all은 고정 문구만 로깅하므로,
        // 예외 메시지는 사용자 입력을 포함할 수 있어 클래스명만 안전하게 보여준다
        String rawMessage = event.getFormattedMessage();
        String messageSource = isGenericAdviceMessage(rawMessage)
                ? throwableClassSummary(throwableProxy)
                : truncate(rawMessage, RAW_MESSAGE_INPUT_LIMIT, "... (input truncated)");
        String messageBody = sanitize(messageSource);
        String description = DIVIDER + "\n" + truncate(messageBody, DESCRIPTION_LIMIT, "\n... (truncated)");
        embed.put("description", description);
        embed.put("color", RED);

        ArrayNode fields = embed.putArray("fields");

        // HTTP 요청 스레드에서 난 에러면 어떤 API 호출이었는지 표시 (RequestContextLoggingFilter가 MDC에 심어둠).
        // 스케줄러/비동기 리스너처럼 요청 컨텍스트가 없는 곳에서 난 에러는 안내 문구로 대체한다
        String requestHandler = event.getMDCPropertyMap().get(RequestContextLoggingFilter.MDC_KEY_HANDLER);
        String safeRequestHandler = sanitize(requestHandler != null && !requestHandler.isBlank()
                ? requestHandler : NO_API_CONTEXT);
        addField(fields, "🌐 요청 Handler", inlineCode(safeRequestHandler), false, HANDLER_VALUE_LIMIT);

        // 발생 시간 / Logger / Thread 순서로 inline(3개) 한 줄에 나란히 배치해서 세로 공간을 아낀다
        addField(fields, "⏰ 발생 시간", inlineCode(TIME_FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp()))), true);
        addField(fields, "Logger", inlineCode(event.getLoggerName()), true, LOGGER_VALUE_LIMIT);
        addField(fields, "Thread", inlineCode(event.getThreadName()), true, THREAD_VALUE_LIMIT);

        if (throwableProxy != null) {
            addField(fields, "📜 Stack Trace", "```\n" + renderStackTrace(throwableProxy) + "\n```", false);
        }

        return root.toString();
    }

    private void addField(ArrayNode fields, String name, String value, boolean inline) {
        addField(fields, name, value, inline, FIELD_VALUE_LIMIT);
    }

    private void addField(ArrayNode fields, String name, String value, boolean inline, int valueLimit) {
        ObjectNode field = fields.addObject();
        field.put("name", name);
        field.put("value", (value == null || value.isBlank()) ? "-" : truncate(value, valueLimit, "..."));
        field.put("inline", inline);
    }

    private String inlineCode(String text) {
        String safeText = text == null || text.isBlank()
                ? "-"
                : text.replace('`', '\'').replace('\r', ' ').replace('\n', ' ');
        return "`" + safeText + "`";
    }

    private boolean isGenericAdviceMessage(String message) {
        return message != null && message.trim().equals(GENERIC_UNHANDLED_MESSAGE);
    }

    private String throwableClassSummary(IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return GENERIC_UNHANDLED_MESSAGE;
        }
        return simpleName(throwableProxy.getClassName());
    }

    private String simpleName(String fqcn) {
        if (fqcn == null || fqcn.isBlank()) {
            return "Unknown";
        }
        int lastDot = fqcn.lastIndexOf('.');
        return lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn;
    }

    private String renderStackTrace(IThrowableProxy throwableProxy) {
        StringBuilder sb = new StringBuilder();
        sb.append(simpleName(throwableProxy.getClassName())).append("\n");
        var frames = throwableProxy.getStackTraceElementProxyArray();
        int frameCount = frames == null ? 0 : Math.min(frames.length, STACK_TRACE_MAX_LINES);
        for (int i = 0; i < frameCount; i++) {
            sb.append(frames[i].getSTEAsString()).append("\n");
        }
        if (frames != null && frames.length > STACK_TRACE_MAX_LINES) {
            sb.append("... ").append(frames.length - STACK_TRACE_MAX_LINES).append(" more frames truncated");
        }

        // 예외 메시지는 제외하고 클래스와 frame만 남긴다. 코드블록을 깨는 백틱도 치환한다.
        String safeTrace = sanitize(sb.toString().stripTrailing()).replace('`', '\'');
        return truncate(safeTrace, FIELD_VALUE_LIMIT - 8, "\n... truncated");
    }

    private String sanitize(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String sanitized = PRIVATE_KEY_BLOCK.matcher(text).replaceAll(MASK);
        sanitized = DISCORD_WEBHOOK.matcher(sanitized).replaceAll("$1" + MASK);
        sanitized = SENSITIVE_HEADER.matcher(sanitized).replaceAll("$1=" + MASK);
        sanitized = JSON_SECRET.matcher(sanitized).replaceAll("$1" + MASK + "$2");
        sanitized = LABELED_SECRET.matcher(sanitized).replaceAll("$1=" + MASK);
        sanitized = BEARER_TOKEN.matcher(sanitized).replaceAll("Bearer " + MASK);
        sanitized = JWT.matcher(sanitized).replaceAll(MASK);
        return EMAIL.matcher(sanitized).replaceAll(MASK);
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
