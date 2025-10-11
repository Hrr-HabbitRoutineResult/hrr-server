package com.hrr.backend.test;

public class TestConverter {

	public static TestResponse.TempDto toTempDto(){
		return TestResponse.TempDto.builder()
			.testString("api 테스트 중입니다.")
			.build();
	}
}
