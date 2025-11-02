package com.hrr.backend.domain.auth.dto;

import lombok.Builder;

public class AuthResponseDto {
    @Builder
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String nickname
    ) {}
}
