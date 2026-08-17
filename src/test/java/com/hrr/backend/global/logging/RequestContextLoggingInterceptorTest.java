package com.hrr.backend.global.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

class RequestContextLoggingInterceptorTest {

    private final RequestContextLoggingInterceptor interceptor = new RequestContextLoggingInterceptor();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void preHandle_replacesRawPathWithControllerHandlerName() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/challenge/123/join");
        HandlerMethod handler = new HandlerMethod(new SampleController(), SampleController.class.getDeclaredMethod("joinChallenge"));

        interceptor.preHandle(request, new MockHttpServletResponse(), handler);

        assertThat(MDC.get(RequestContextLoggingFilter.MDC_KEY_HANDLER))
                .isEqualTo("POST SampleController.joinChallenge")
                .doesNotContain("123");
    }

    private static class SampleController {
        void joinChallenge() {
        }
    }
}
