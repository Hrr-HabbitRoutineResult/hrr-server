package com.hrr.backend.domain.auth.controller;

import com.hrr.backend.domain.auth.dto.KakaoTokenResponse;
import com.hrr.backend.domain.auth.dto.KakaoUserResponse;
import com.hrr.backend.domain.auth.service.KakaoAuthService;
import com.hrr.backend.domain.auth.service.SocialUserService;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoAuthService kakaoAuthService;
    private final SocialUserService socialUserService;
    private final JwtService JwtService;

    @PostMapping("/kakao/callback")
    public ResponseEntity<?> kakao(@RequestBody Map<String,String> body) {
        String code = body.get("code");

        // code → 토큰
        KakaoTokenResponse token = kakaoAuthService.exchangeToken(code);

        // 토큰 → 유저정보
        KakaoUserResponse kakaoUser = kakaoAuthService.fetchUser(token.getAccessToken());

        // upsert
        User user = socialUserService.upsertKakaoUser(kakaoUser);

        // (임시) 우리 AccessToken 발급
        String access = JwtService.generateAccessToken(user.getId());

        return ResponseEntity.ok(Map.of(
                "accessToken", access,
                "userId", user.getId(),
                "nickname", user.getNickname()
        ));
    }
}

