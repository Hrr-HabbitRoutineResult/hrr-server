package com.hrr.backend.domain.auth.service;

import java.time.Duration;
import java.util.Map;

import com.hrr.backend.domain.auth.dto.AuthRequestDto;
import com.hrr.backend.domain.auth.dto.AuthResponseDto;
import com.hrr.backend.domain.auth.dto.KakaoTokenResponse;
import com.hrr.backend.domain.auth.dto.KakaoUserResponse;
import com.hrr.backend.domain.auth.dto.NaverUserResponse;
import com.hrr.backend.domain.auth.entity.SocialAuth;
import com.hrr.backend.domain.auth.entity.enums.SocialType;
import com.hrr.backend.domain.auth.repository.SocialAuthRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoAuthService kakaoAuthService;
    private final AppleAuthService appleAuthService;
    private final NaverAuthService naverAuthService;
    private final SocialUserService socialUserService;
    private final JwtService jwtService;

    private final SocialAuthRepository socialAuthRepository;
    private final UserRepository userRepository;
    private final UserChallengeRepository userChallengeRepository;

    public AuthResponseDto.LoginResponse socialLogin(SocialType socialType, AuthRequestDto.SocialLoginRequest request) {
        if (socialType != SocialType.KAKAO) {
            throw new GlobalException(ErrorCode.AUTH_UNSUPPORTED_SOCIAL_TYPE);
        }

        try {
            KakaoTokenResponse token = kakaoAuthService.exchangeToken(request.code());
            KakaoUserResponse kakaoUser = kakaoAuthService.fetchUser(token.getAccessToken());
            User user = socialUserService.upsertKakaoUser(kakaoUser);

            String accessToken = jwtService.generateAccessToken(user.getId());
            String refreshToken = jwtService.generateRefreshToken(user.getId());

            String nextStep = user.determineNextStep();

            return new AuthResponseDto.LoginResponse(
                    user.getId(),
                    accessToken,
                    refreshToken,
                    user.getDisplayName(),
                    user.getDisplayNickname(),
                    user.getLoginStatus(),
                    nextStep
            );
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            throw new GlobalException(ErrorCode.AUTH_EXTERNAL_API_ERROR);
        }
    }

    /** Refresh Token 기반 Access Token 재발급 */
    public AuthResponseDto.TokenReissueResponse reissueToken(String refreshHeader) {
        String refreshToken = refreshHeader.startsWith("Bearer ")
                ? refreshHeader.substring(7)
                : refreshHeader;

        // Refresh Token 유효성 검증
        jwtService.validateToken(refreshToken);

        // Redis에 저장된 Refresh Token과 비교 검증
        if (jwtService.isTokenBlacklisted(refreshToken)) {
            throw new GlobalException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        // userId 추출
        Long userId = jwtService.extractUserId(refreshToken);

        if (!jwtService.validateRefreshToken(refreshToken, userId)) {
            throw new GlobalException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        // 새로운 Access Token 발급
        String newAccessToken = jwtService.generateAccessToken(userId);

        // 새로운 Refresh Token 발급 및 기존 RT 교체
        String newRefreshToken = jwtService.generateRefreshToken(userId);

        // 기존 Refresh Token 블랙리스트 처리 (만료되지 않은 경우에만)
        Duration remainingExpiration = jwtService.getRemainingExpiration(refreshToken);
        if (!remainingExpiration.isNegative() && !remainingExpiration.isZero()) {
            jwtService.blacklistToken(refreshToken, remainingExpiration);
        }

        return new AuthResponseDto.TokenReissueResponse(newAccessToken, newRefreshToken);
    }

    public AuthResponseDto.LoginResponse kakaoLogin(String kakaoAccessToken) {

        try {
            KakaoUserResponse kakaoUser = kakaoAuthService.fetchUser(kakaoAccessToken);
            User user = socialUserService.upsertKakaoUser(kakaoUser);

            String accessToken = jwtService.generateAccessToken(user.getId());
            String refreshToken = jwtService.generateRefreshToken(user.getId());

            String nextStep = user.determineNextStep();

            return new AuthResponseDto.LoginResponse(
                    user.getId(),
                    accessToken,
                    refreshToken,
                    user.getDisplayName(),
                    user.getDisplayNickname(),
                    user.getLoginStatus(),
                    nextStep
            );
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            throw new GlobalException(ErrorCode.AUTH_EXTERNAL_API_ERROR);
        }
    }

    public AuthResponseDto.LoginResponse appleLogin(AuthRequestDto.AppleLoginRequest request) {

        try {
            Map<String, String> appleTokens = appleAuthService.getAppleTokens(request.getAuthorizationCode());

            if (appleTokens == null || appleTokens.get("id_token") == null) {
                log.error("애플 id_token이 유효하지 않습니다.");
                throw new GlobalException(ErrorCode.AUTH_APPLE_TOKEN_ERROR);
            }

            String socialId = appleAuthService.getAppleAccountId(appleTokens.get("id_token"));
            String appleRefreshToken = appleTokens.get("refresh_token");

            User user = socialUserService.upsertAppleUser(socialId, appleRefreshToken, request.getName());

            String accessToken = jwtService.generateAccessToken(user.getId());
            String refreshToken = jwtService.generateRefreshToken(user.getId());

            String nextStep = user.determineNextStep();

            return new AuthResponseDto.LoginResponse(
                    user.getId(),
                    accessToken,
                    refreshToken,
                    user.getDisplayName(),
                    user.getDisplayNickname(),
                    user.getLoginStatus(),
                    nextStep
            );
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            throw new GlobalException(ErrorCode.AUTH_EXTERNAL_API_ERROR);
        }
    }

    public AuthResponseDto.LoginResponse naverLogin(String naverAccessToken, String naverRefreshToken) {

        try {
            NaverUserResponse naverUser = naverAuthService.fetchUser(naverAccessToken);
            User user = socialUserService.upsertNaverUser(naverUser, naverRefreshToken);

            String accessToken = jwtService.generateAccessToken(user.getId());
            String refreshToken = jwtService.generateRefreshToken(user.getId());

            String nextStep = user.determineNextStep();

            return new AuthResponseDto.LoginResponse(
                    user.getId(),
                    accessToken,
                    refreshToken,
                    user.getDisplayName(),
                    user.getDisplayNickname(),
                    user.getLoginStatus(),
                    nextStep
            );
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            log.error("네이버 로그인 중 오류 발생: ", e);
            throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
        }
    }

    /** 로그아웃 */
    public void logout(String tokenHeader) {
        String token = tokenHeader.startsWith("Bearer ")
                ? tokenHeader.substring(7)
                : tokenHeader;

        // userId 추출
        Long userId = jwtService.extractUserId(token);

        // Access Token 블랙리스트 처리
        Duration remainingExpiration = jwtService.getRemainingExpiration(token);
        if (!remainingExpiration.isNegative() && !remainingExpiration.isZero()) {
            jwtService.blacklistToken(token, remainingExpiration);
        }

        // Refresh Token 삭제 (Redis에서 제거)
        jwtService.deleteRefreshToken(userId);
    }

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        user.withdraw();

        // 탈퇴 시 Refresh Token 삭제
        jwtService.deleteRefreshToken(userId);
    }

    @Transactional
    public void revoke(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        SocialAuth socialAuth = socialAuthRepository.findByUser(user)
                .orElseThrow(() -> new GlobalException(ErrorCode.AUTH_INFO_NOT_FOUND));

        switch (socialAuth.getSocialType()) {
            case NAVER -> naverAuthService.revoke(socialAuth.getSocialRefreshToken());
            case APPLE -> appleAuthService.revoke(socialAuth.getSocialRefreshToken());
            case KAKAO -> kakaoAuthService.unlink(socialAuth.getSocialId());
            default -> throw new GlobalException(ErrorCode.AUTH_INVALID_SOCIAL_TYPE);
        }
        jwtService.deleteRefreshToken(userId);
    }

}