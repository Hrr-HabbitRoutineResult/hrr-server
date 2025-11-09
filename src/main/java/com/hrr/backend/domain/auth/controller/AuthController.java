//package com.hrr.backend.domain.auth.controller;
//
//import com.hrr.backend.domain.auth.dto.AuthRequestDto;
//import com.hrr.backend.domain.auth.dto.AuthResponseDto;
//import com.hrr.backend.domain.auth.entity.enums.SocialType;
//import com.hrr.backend.domain.auth.service.AuthService;
//import com.hrr.backend.global.response.SuccessCode;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.web.bind.annotation.*;
//
//@Tag(name = "Auth", description = "인증 관련 API")
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/api/v1/auth")
//public class AuthController {
//
//    private final AuthService authService;
//
//    @Operation(
//            summary = "소셜 로그인",
//            description = "카카오, 네이버, 애플 소셜 로그인 중 선택하여 로그인합니다. (현재는 Kakao만 지원)"
//    )
//    @PostMapping(value = "/social-login/{socialType}", produces = "application/json")
//    public com.hrr.backend.global.response.ApiResponse<AuthResponseDto.LoginResponse> socialLogin(
//            @Parameter(
//                    description = "소셜 로그인 타입 (KAKAO, NAVER, APPLE)",
//                    required = true,
//                    schema = @Schema(type = "string", allowableValues = {"KAKAO", "NAVER", "APPLE"}, example = "KAKAO")
//            )
//            @PathVariable("socialType") SocialType socialType,
//            @Valid @RequestBody AuthRequestDto.SocialLoginRequest request
//    ) {
//        AuthResponseDto.LoginResponse response = authService.socialLogin(socialType, request);
//        return com.hrr.backend.global.response.ApiResponse.onSuccess(SuccessCode.OK, response);
//    }
//}

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
    /**
     * 카카오 인가코드 콜백 테스트용 -> swagger에서 돌리지 말고 여기서 받으시면 됩니당
     * ex) http://localhost:8080/api/v1/auth/kakao/callback?code=xxxxx
     */
    @GetMapping("/kakao/callback")
    @Operation(summary = "카카오 인가코드 콜백", description = "카카오 로그인 후 인가코드를 수신합니다. (테스트용)")
    public ApiResponse<AuthResponseDto.LoginResponse> kakaoCallback(
            @RequestParam("code") String code
    ) {
        // 인가코드를 받아 로그인 처리 진행
        AuthRequestDto.SocialLoginRequest request = new AuthRequestDto.SocialLoginRequest(code);
        AuthResponseDto.LoginResponse response = authService.socialLogin(SocialType.KAKAO, request);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
    /** 소셜 로그인 */
    @Operation(summary = "소셜 로그인", description = "카카오, 네이버, 애플 소셜 로그인 (현재 Kakao만 지원)")
    @PostMapping(value = "/social-login/{socialType}", produces = "application/json")
    public ApiResponse<AuthResponseDto.LoginResponse> socialLogin(
            @Parameter(
                    description = "소셜 로그인 타입 (KAKAO, NAVER, APPLE)",
                    required = true,
                    schema = @Schema(type = "string", allowableValues = {"KAKAO", "NAVER", "APPLE"}, example = "KAKAO")
            )
            @PathVariable("socialType") SocialType socialType,
            @Valid @RequestBody AuthRequestDto.SocialLoginRequest request
    ) {
        AuthResponseDto.LoginResponse response = authService.socialLogin(socialType, request);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    /**  Refresh Token으로 Access Token 재발급 */
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 Access Token을 재발급합니다.")
    @PostMapping(value = "/reissue", produces = "application/json")
    public ApiResponse<AuthResponseDto.TokenReissueResponse> reissueToken(
            @RequestHeader("Authorization") String refreshToken
    ) {
        AuthResponseDto.TokenReissueResponse response = authService.reissueToken(refreshToken);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}

