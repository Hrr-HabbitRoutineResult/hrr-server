package com.hrr.backend.test;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

public class TestRequest {

	@Getter
	@Schema(description = "테스트 요청 Dto")
	public static class TestDto{
		@Schema(description = "플래그", example = "1")
		private int flag;
	}
}
