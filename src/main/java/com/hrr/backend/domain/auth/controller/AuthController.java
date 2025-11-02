package com.hrr.backend.domain.auth.controller;

import com.hrr.backend.domain.auth.dto.KakaoTokenResponse;
import com.hrr.backend.domain.auth.dto.KakaoUserResponse;
import com.hrr.backend.domain.auth.service.KakaoAuthService;
import com.hrr.backend.domain.auth.service.SocialUserService;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.auth.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoAuthService kakaoAuthService;
    private final SocialUserService socialUserService;
    private final JwtService JwtService;

    /**
     * 프론트에서 전달받은 인가코드로 카카오 로그인 처리
     */
    @Operation(summary = "카카오 로그인", description = "프론트/앱에서 받은 인가코드를 사용해 카카오 로그인 후 JWT를 발급합니다.")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @PostMapping("/kakao/login")
    public ResponseEntity<?> kakaoLogin(@RequestParam("code") String code) {
        // 인가코드로 액세스 토큰 요청
        KakaoTokenResponse token = kakaoAuthService.exchangeToken(code);

        // 카카오 유저 정보 조회
        KakaoUserResponse kakaoUser = kakaoAuthService.fetchUser(token.getAccessToken());

        // 우리 DB에 유저 upsert
        User user = socialUserService.upsertKakaoUser(kakaoUser);

        // 우리 서비스 JWT 생성
        String accessToken = JwtService.generateAccessToken(user.getId());
        String refreshToken = JwtService.generateRefreshToken(user.getId());

        // 결과 반환
        return ResponseEntity.ok().body(
                new LoginResponse(accessToken, refreshToken, user.getNickname())
        );
    }

    // 응답 DTO (Swagger에서도 자동 표시됨)
    record LoginResponse(String accessToken, String refreshToken, String nickname) {}

}

