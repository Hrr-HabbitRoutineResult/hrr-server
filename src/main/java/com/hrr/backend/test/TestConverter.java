package com.hrr.backend.test;

public class TestConverter {

	public static TestResponse.TempDto toTempDto(){
		return TestResponse.TempDto.builder()
			.testString("api 테스트 중입니다.")
			.build();
	}

	public static TestResponse.ExceptionDto toExceptionDto(){
		return TestResponse.ExceptionDto.builder()
			.content("에러 발생 테스트")
			.build();
	}
}
