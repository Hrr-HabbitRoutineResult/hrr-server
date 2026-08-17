package com.hrr.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.hrr.backend.global.logging.RequestContextLoggingInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class LoggingWebMvcConfig implements WebMvcConfigurer {

    private final RequestContextLoggingInterceptor requestContextLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestContextLoggingInterceptor);
    }
}
