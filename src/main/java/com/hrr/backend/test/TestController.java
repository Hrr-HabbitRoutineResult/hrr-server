package com.hrr.backend.test;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrr.backend.response.ApiResponse;
import com.hrr.backend.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Temp", description = "테스트 API")
@RestController
@RequestMapping("api/v1/test")
@RequiredArgsConstructor
public class TestController {

	private final TestService testService;

	@GetMapping("/temp")
	@Operation(summary = "테스트 API", description = "테스트용 임시 API 입니다.")
	public ApiResponse<TestResponse.TempDto> testAPI(){

		return ApiResponse.onSuccess(SuccessCode.OK, TestConverter.toTempDto());
	}

	@PostMapping("/exception")
	@Operation(summary = "테스트 API_2", description = "예외 발생 확인 API 입니다.")
	public ApiResponse<TestResponse.ExceptionDto> exceptionAPI(@RequestBody TestRequest.TestDto request){
		testService.CheckFlag(request.getFlag());

		return ApiResponse.onSuccess(SuccessCode.OK, TestConverter.toExceptionDto());
	}

}
