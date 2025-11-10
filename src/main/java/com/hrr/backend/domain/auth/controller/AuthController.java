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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

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

    /** Swagger 테스트용 카카오 콜백 */
    @GetMapping("/kakao/callback")
    @Operation(summary = "카카오 인가코드 콜백 (Swagger 테스트용)",
            description = "카카오 로그인 후 인가코드를 수신하고 바로 로그인 처리합니다.")
    public ApiResponse<AuthResponseDto.LoginResponse> kakaoCallback(
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

