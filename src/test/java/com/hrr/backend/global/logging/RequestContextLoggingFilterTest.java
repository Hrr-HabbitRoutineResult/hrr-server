package com.hrr.backend.global.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletException;

class RequestContextLoggingFilterTest {

    private final RequestContextLoggingFilter filter = new RequestContextLoggingFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void doFilter_setsOnlyMethodAsSafeFallback_andClearsAfterward() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/challenges");
        request.setQueryString("token=must-not-be-captured");
        AtomicReference<String> capturedApi = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> capturedApi.set(MDC.get(RequestContextLoggingFilter.MDC_KEY_HANDLER)));

        assertThat(capturedApi.get()).isEqualTo("GET");
        assertThat(capturedApi.get()).doesNotContain("token");
        assertThat(MDC.get(RequestContextLoggingFilter.MDC_KEY_HANDLER)).isNull();
    }

    @Test
    void doFilter_clearsMdc_whenDownstreamThrows() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/test");

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> {
                    throw new ServletException("failure");
                }))
                .isInstanceOf(ServletException.class);

        assertThat(MDC.get(RequestContextLoggingFilter.MDC_KEY_HANDLER)).isNull();
    }
}
