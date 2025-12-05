package com.hrr.backend.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

public class AuthRequestDto {
    public record SocialLoginRequest(
            @NotBlank(message = "인가 코드는 필수입니다.")
            String code
    ) {}

	@Getter
	public static class KakaoAccessTokenDto {
		@Schema(description = "카카오 SDK에서 발급받은 Access Token", example = "string")
		@NotBlank(message = "토큰은 필수값입니다.")
		String accessToken;
	}
}
