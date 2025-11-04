package com.hrr.backend.domain.auth.service;

import com.hrr.backend.domain.auth.dto.AuthRequestDto;
import com.hrr.backend.domain.auth.dto.AuthResponseDto;
import com.hrr.backend.domain.auth.dto.KakaoTokenResponse;
import com.hrr.backend.domain.auth.dto.KakaoUserResponse;

import com.hrr.backend.domain.auth.entity.enums.SocialType;
import com.hrr.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoAuthService kakaoAuthService;
    private final SocialUserService socialUserService;
    private final JwtService jwtService;

    public AuthResponseDto.LoginResponse socialLogin(SocialType socialType, AuthRequestDto.SocialLoginRequest request) {
        if (socialType != SocialType.KAKAO) {
            throw new IllegalArgumentException("현재는 kakao 로그인만 지원합니다.");
        }

        // 1. 카카오 인가 코드로 토큰 발급
        KakaoTokenResponse token = kakaoAuthService.exchangeToken(request.code());

        // 2. 카카오 유저 정보 조회
        KakaoUserResponse kakaoUser = kakaoAuthService.fetchUser(token.getAccessToken());

        // 3. DB에 유저 upsert
        User user = socialUserService.upsertKakaoUser(kakaoUser);

        // 4. JWT 생성
        String accessToken = jwtService.generateAccessToken(user.getId());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // 5. 응답 DTO로 반환
        return new AuthResponseDto.LoginResponse(accessToken, refreshToken, user.getNickname());
    }
}
