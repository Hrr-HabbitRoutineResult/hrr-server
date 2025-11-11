package com.hrr.backend.global.config.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException)
            throws IOException, ServletException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");

        ApiResponse<Object> body = ApiResponse.onFailure(
                ErrorCode.AUTH_INVALID_TOKEN,
                null
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
