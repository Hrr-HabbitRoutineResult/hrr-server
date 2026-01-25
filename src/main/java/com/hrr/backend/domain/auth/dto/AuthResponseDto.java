package com.hrr.backend.domain.auth.dto;

import com.hrr.backend.domain.user.entity.enums.LoginStatus;
import lombok.Builder;

public class AuthResponseDto {

    @Builder
    public record LoginResponse(
            Long userId,
            String accessToken,
            String refreshToken,
            String name,
            String nickname,
            LoginStatus loginStatus,
            String nextStep
    ) {}

    @Builder
    public record TokenReissueResponse(
            String accessToken,
            String refreshToken  // 추가: 새로운 Refresh Token도 함께 반환
    ) {}

}