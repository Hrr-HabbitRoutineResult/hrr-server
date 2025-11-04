package com.hrr.backend.domain.auth.controller;

import com.hrr.backend.domain.auth.dto.AuthRequestDto;
import com.hrr.backend.domain.auth.dto.AuthResponseDto;
import com.hrr.backend.domain.auth.entity.enums.SocialType;
import com.hrr.backend.domain.auth.service.AuthService;
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

    @Operation(
            summary = "소셜 로그인",
            description = "카카오, 네이버, 애플 소셜 로그인 중 선택하여 로그인합니다. (현재는 Kakao만 지원)"
    )
    @PostMapping(value = "/social-login/{socialType}", produces = "application/json")
    public com.hrr.backend.global.response.ApiResponse<AuthResponseDto.LoginResponse> socialLogin(
            @Parameter(
                    description = "소셜 로그인 타입 (KAKAO, NAVER, APPLE)",
                    required = true,
                    schema = @Schema(type = "string", allowableValues = {"KAKAO", "NAVER", "APPLE"}, example = "KAKAO")
            )
            @PathVariable("socialType") SocialType socialType,
            @Valid @RequestBody AuthRequestDto.SocialLoginRequest request
    ) {
        AuthResponseDto.LoginResponse response = authService.socialLogin(socialType, request);
        return com.hrr.backend.global.response.ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}
