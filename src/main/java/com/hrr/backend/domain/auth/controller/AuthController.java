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
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

        log.info("Kakao Callback Received with code: {}", code);

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

        log.info("Login Success! Redirecting to App: {}", redirectUrl);

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
}

