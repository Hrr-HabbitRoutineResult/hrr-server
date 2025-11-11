package com.hrr.backend.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthRequestDto {
    public record SocialLoginRequest(
            @NotBlank(message = "인가 코드는 필수입니다.")
            String code
    ) {}
}
