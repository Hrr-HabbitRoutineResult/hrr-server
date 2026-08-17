package com.hrr.backend.test;

import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 로컬 개발/테스트 전용 API 모음.
 * local 프로필에서만 빈이 등록되므로 prod/test 빌드에는 컨트롤러 자체가 존재하지 않는다(=Swagger에도 안 뜸).
 * 로컬 Swagger에서는 편의를 위해 그대로 노출한다.
 */
@Profile("local")
@Tag(name = "Temp", description = "테스트 API")
@Slf4j
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

	/** Discord 웹훅 에러 알림 파이프라인이 실제로 동작하는지 확인용. 강제로 예외를 던져 ExceptionAdvice의
	 * catch-all(log.error)을 거쳐 Discord로 전송되는지 끝까지 확인할 수 있다. */
	@GetMapping("/discord-error")
	@Operation(summary = "Discord 웹훅 알림 테스트", description = "강제로 예외를 발생시켜 Discord 에러 알림이 오는지 확인합니다.")
	public void discordAlertTest() {
		throw new RuntimeException("[discordAlertTest] Discord 웹훅 알림 테스트용 강제 에러입니다.");
	}

	// 명시적으로 허용한 진단용 항목 외에는 값의 성격을 추측하지 않고 전부 마스킹한다.
	private static final List<String> SAFE_VALUE_NAMES =
			List.of("content-type", "user-agent", "accept");

	/** 애플 로그인 테스트용 임시 리다이렉트 url. 애플이 실제로 보내는 헤더/파라미터를 로그로 남긴다(민감한 값은 마스킹). */
	@PostMapping("/apple-callback")
	@Operation(summary = "애플 로그인 테스트용 url")
	public void appleTestCallback(HttpServletRequest request) {
		// 헤더 전체 출력
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			log.info("[appleTestCallback] 요청 header를 확인합니다. name={}, value={}",
				name, maskIfSensitive(name, request.getHeader(name)));
		}

		// 파라미터(Body) 전체 출력
		Enumeration<String> params = request.getParameterNames();
		while (params.hasMoreElements()) {
			String name = params.nextElement();
			log.info("[appleTestCallback] 요청 parameter를 확인합니다. name={}, value={}",
				name, maskIfSensitive(name, request.getParameter(name)));
		}

	}

	String maskIfSensitive(String name, String value) {
		String lowerName = name.toLowerCase(Locale.ROOT);
		boolean safe = SAFE_VALUE_NAMES.stream().anyMatch(lowerName::equals);
		return safe ? value : "***MASKED***";
	}

}
