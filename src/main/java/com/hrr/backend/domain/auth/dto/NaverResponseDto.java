package com.hrr.backend.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class NaverResponseDto {

	@Getter
	@NoArgsConstructor
	public static class NaverRevokeResponse {
		private String result;
		@JsonProperty("error")
		private String error;
		@JsonProperty("error_description")
		private String errorDescription;
	}

	@Getter
	@NoArgsConstructor
	@JsonIgnoreProperties(ignoreUnknown = true) // 정의하지 않은 다른 필드들은 무시함
	public static class NaverTokenResponse {
		@JsonProperty("access_token")
		private String accessToken;

		@JsonProperty("refresh_token")
		private String refreshToken;

		@JsonProperty("token_type")
		private String tokenType;

		@JsonProperty("expires_in")
		private Long expiresIn;
	}
}
