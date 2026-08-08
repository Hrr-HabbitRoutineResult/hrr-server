package com.hrr.backend.global.logging;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청 처리 중 발생한 ERROR 로그가 "어떤 API 호출에서 났는지" Discord 알림에 표시할 수 있도록 MDC를 준비한다.
 * 이 필터에서는 개인정보가 포함될 수 있는 raw URI를 넣지 않고 HTTP method만 안전한 fallback으로 설정한다.
 * HandlerMapping 이후에는 RequestContextLoggingInterceptor가 URL 대신 controller handler 식별자로 덮어쓴다.
 * Logback은 이벤트 생성 시점에 MDC 스냅샷을 이벤트에 함께 저장하므로,
 * 비동기 appender(AsyncAppender)를 거쳐도 값이 유실되지 않는다.
 * DiscordEmbedPayloadBuilder가 이 MDC 값을 읽어 필드로 추가한다 (스케줄러/비동기 리스너처럼
 * HTTP 요청이 아닌 곳에서 난 에러는 이 값이 없어 대신 안내 문구가 표시된다).
 */
@Component
public class RequestContextLoggingFilter extends OncePerRequestFilter {

    public static final String MDC_KEY_HANDLER = "requestHandler";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        MDC.put(MDC_KEY_HANDLER, request.getMethod());
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 스레드 풀 재사용 시 다음 요청으로 값이 새지 않도록 반드시 정리
            MDC.remove(MDC_KEY_HANDLER);
        }
    }
}
