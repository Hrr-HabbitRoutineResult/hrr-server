package com.hrr.backend.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NaverUserResponse {
	private String resultCode;
	private String message;
	private Response response;

	@Getter
	@NoArgsConstructor
	public static class Response {
		private String id;           // 네이버 고유 식별자
		private String email;
		private String name;
		private String profileImage;
	}
}
