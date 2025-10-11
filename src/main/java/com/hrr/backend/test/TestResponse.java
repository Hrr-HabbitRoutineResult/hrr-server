package com.hrr.backend.test;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class TestResponse {

	@Builder
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TempDto{
		String testString;
	}

	@Builder
	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ExceptionDto{
		String content;
	}
}
