package com.hrr.backend.global.logging;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** URL 대신 controller class와 method 이름을 ERROR 로그의 요청 문맥으로 사용한다. */
@Component
public class RequestContextLoggingInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
            String handlerName = handlerMethod.getBeanType().getSimpleName()
                    + "." + handlerMethod.getMethod().getName();
            MDC.put(RequestContextLoggingFilter.MDC_KEY_HANDLER, request.getMethod() + " " + handlerName);
        }
        return true;
    }
}
