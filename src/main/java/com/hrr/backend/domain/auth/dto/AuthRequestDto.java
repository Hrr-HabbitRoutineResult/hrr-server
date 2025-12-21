package com.hrr.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

	@Getter
	@NoArgsConstructor
	public static class AppleLoginRequest {
		@Schema(description = "인가 코드(토큰 발급용)", example = "string")
		@NotBlank(message = "코드는 필수값입니다.")
		private String authorizationCode;

		private String firstName;
		private String lastName;

		@JsonIgnore
		@Schema(hidden = true)
		public String getName() {
			return lastName + firstName;
		}
	}
}
