package com.hrr.backend.domain.auth.controller;

import com.hrr.backend.domain.auth.dto.AuthRequestDto;
import com.hrr.backend.domain.auth.dto.AuthResponseDto;
import com.hrr.backend.domain.auth.entity.enums.SocialType;
import com.hrr.backend.domain.auth.service.AuthService;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @Value("${kakao.app-redirect-uri}")
    private String appRedirectUri;

    /** 프론트에서 인가코드 받아 백엔드로 전달하는 로그인 */
    @Operation(summary = "소셜 로그인", description = "프론트에서 카카오 인가 코드를 받아 로그인 요청합니다.")
    @PostMapping(value = "/social-login/{socialType}", produces = "application/json")
    public ApiResponse<AuthResponseDto.LoginResponse> socialLogin(
            @Parameter(
                    description = "소셜 로그인 타입 (KAKAO, NAVER, APPLE)",
                    required = true,
                    schema = @Schema(implementation = SocialType.class, example = "KAKAO")
            )
            @PathVariable("socialType") SocialType socialType,
            @Valid @RequestBody AuthRequestDto.SocialLoginRequest request
    ) {
        AuthResponseDto.LoginResponse response = authService.socialLogin(socialType, request);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @GetMapping("/kakao/callback")
    @Operation(summary = "카카오 인가코드 콜백 (앱 딥링크 리다이렉트)",
            description = "카카오 로그인 후 백엔드가 인가코드를 받아 처리하고, 앱 딥링크로 리다이렉트하여 토큰을 전달합니다.")
    public void kakaoCallback(
            @RequestParam("code") String code,
            HttpServletResponse response
    ) throws IOException {

        log.info("Kakao Callback received. Code received successfully.");

        // 서비스 로직 호출 (인가 코드로 토큰 발급 및 유저 처리)
        AuthRequestDto.SocialLoginRequest requestDto = new AuthRequestDto.SocialLoginRequest(code);
        AuthResponseDto.LoginResponse loginResponse = authService.socialLogin(SocialType.KAKAO, requestDto);

        // 앱으로 돌아갈 딥링크 URL 생성
        String redirectUrl = UriComponentsBuilder.fromUriString(appRedirectUri)
                .queryParam("accessToken", loginResponse.accessToken())
                .queryParam("refreshToken", loginResponse.refreshToken())
                .queryParam("userId", loginResponse.userId())
                .queryParam("nickname", loginResponse.nickname())
                .queryParam("loginStatus", loginResponse.loginStatus())
                .queryParam("nextStep", loginResponse.nextStep())
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        log.info("Login Success! Redirecting to app deep link");

        // 리다이렉트 전송 (302 Found) -> 앱 실행.
        response.sendRedirect(redirectUrl);
    }

    /** Swagger 테스트용 카카오 콜백 */
    @GetMapping("/kakao/callback/swagger")
    @Operation(summary = "카카오 인가코드 콜백 (Swagger 테스트용)",
            description = "카카오 로그인 후 인가코드를 수신하고 바로 로그인 처리합니다.")
    public ApiResponse<AuthResponseDto.LoginResponse> kakaoCallbackSwagger(
            @Valid @ModelAttribute AuthRequestDto.SocialLoginRequest request
    ) {
        AuthResponseDto.LoginResponse response = authService.socialLogin(SocialType.KAKAO, request);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    /** Refresh Token으로 Access Token 재발급 */
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token을 재발급합니다.")
    @PostMapping(value = "/reissue", produces = "application/json")
    public ApiResponse<AuthResponseDto.TokenReissueResponse> reissueToken(
            @RequestHeader("Authorization") String refreshToken
    ) {
        AuthResponseDto.TokenReissueResponse response = authService.reissueToken(refreshToken);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

	/**
	 * 카카오 엑세스 토큰을 통해 로그인
	 * 카카오 Access Token을 받아, 우리 서비스의 JWT를 반환합니다.
	 */
	@PostMapping("/login/kakao")
	@Operation(summary = "카카오 로그인 (SDK 방식)",
		description = "전달받은 카카오 Access Token으로 유저 정보를 조회하고 로그인/회원가입 처리합니다.")
	public ApiResponse<AuthResponseDto.LoginResponse> kakaoLoginByToken(
		@RequestBody @Valid AuthRequestDto.KakaoAccessTokenDto request) {

		// 서비스 로직 호출
		AuthResponseDto.LoginResponse loginResponse = authService.kakaoLogin(request.getAccessToken());

		return ApiResponse.onSuccess(SuccessCode.OK, loginResponse);
	}

	@PostMapping("/login/apple")
	@Operation(summary = "애플 로그인",
		description = "authorizationCode와 이름 정보를 받아 애플 로그인을 구현합니다. 이름은 애플 응답 그대로 first name과 last name을 넣어주시면 됩니다.")
	public ApiResponse<AuthResponseDto.LoginResponse> appleLogin(
		@RequestBody @Valid AuthRequestDto.AppleLoginRequest request) {

		return ApiResponse.onSuccess(SuccessCode.OK, authService.appleLogin(request));
	}

	/**
	 * 네이버 엑세스 토큰을 통해 로그인 (SDK 방식)
	 */
	@PostMapping("/login/naver")
	@Operation(summary = "네이버 로그인 (SDK 방식)",
		description = "전달받은 네이버 Access Token으로 로그인/회원가입 처리")
	public ApiResponse<AuthResponseDto.LoginResponse> naverLoginByToken(
		@RequestBody @Valid AuthRequestDto.NaverAccessTokenDto request) {

		return ApiResponse.onSuccess(SuccessCode.OK, authService.naverLogin(request.getAccessToken(), request.getRefreshToken()));
	}

	@PostMapping("/logout")
	@Operation(summary = "로그아웃",
		description = "로그아웃 시 토큰을 무효화 시킵니다.")
	public ApiResponse<String> logout(@RequestHeader("Authorization") String authorizationHeader) {

		authService.logout(authorizationHeader);

		return ApiResponse.onSuccess(SuccessCode.OK, "로그아웃에 성공했습니다.");
	}

	/**
	 * 애플 로그인 테스트용 임시 리다이렉트 url
	 */
	@Profile("local")	// 로켈 테스트 환경에서만 작동 제한
	@PostMapping("/login/apple/test")
	@Operation(summary = "애플 로그인 테스트용 url")
	public void appleTestCallback(jakarta.servlet.http.HttpServletRequest request) {
		log.info("======= [DEBUG] APPLE CALLBACK START =======");

		// 헤더 전체 출력
		java.util.Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			log.info("Header -> {}: {}", name, request.getHeader(name));
		}

		// 파라미터(Body) 전체 출력
		java.util.Enumeration<String> params = request.getParameterNames();
		while (params.hasMoreElements()) {
			String name = params.nextElement();
			log.info("Param -> {}: {}", name, request.getParameter(name));
		}

		log.info("======= [DEBUG] APPLE CALLBACK END =======");
	}
}

