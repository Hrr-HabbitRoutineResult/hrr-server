package com.hrr.backend.domain.auth.service;

import com.hrr.backend.domain.auth.dto.AuthRequestDto;
import com.hrr.backend.domain.auth.dto.AuthResponseDto;
import com.hrr.backend.domain.auth.dto.KakaoTokenResponse;
import com.hrr.backend.domain.auth.dto.KakaoUserResponse;
import com.hrr.backend.domain.auth.entity.enums.SocialType;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoAuthService kakaoAuthService;
    private final SocialUserService socialUserService;
    private final JwtService jwtService;

    public AuthResponseDto.LoginResponse socialLogin(SocialType socialType, AuthRequestDto.SocialLoginRequest request) {
        // 지원하지 않는 소셜 타입이면 GlobalException 던지기
        if (socialType != SocialType.KAKAO) {
            throw new GlobalException(ErrorCode.AUTH_UNSUPPORTED_SOCIAL_TYPE);
        }

        try {
            // 1. 카카오 인가 코드로 토큰 발급
            KakaoTokenResponse token = kakaoAuthService.exchangeToken(request.code());

            // 2. 카카오 유저 정보 조회
            KakaoUserResponse kakaoUser = kakaoAuthService.fetchUser(token.getAccessToken());

            // 3. DB에 유저 upsert
            User user = socialUserService.upsertKakaoUser(kakaoUser);

            // 4. JWT 생성
            String accessToken = jwtService.generateAccessToken(user.getId());
            String refreshToken = jwtService.generateRefreshToken(user.getId());

            // 5. 다음단계 게산
            String nextStep = user.determineNextStep();

            return new AuthResponseDto.LoginResponse(
                    user.getId(),
                    accessToken,
                    refreshToken,
                    user.getNickname(),
                    user.getLoginStatus(),
                    nextStep
            );
        } catch (GlobalException e) {
            // KakaoAuthService 내부에서 GlobalException 이미 던지면 그대로 전달
            throw e;
        } catch (Exception e) {
            // 외부 카카오 서버 통신 오류 처리
            throw new GlobalException(ErrorCode.AUTH_EXTERNAL_API_ERROR);
        }
    }
    /** Refresh Token 기반 Access Token 재발급 */
    public AuthResponseDto.TokenReissueResponse reissueToken(String refreshHeader) {
        // "Bearer " 접두사 제거
        String refreshToken = refreshHeader.startsWith("Bearer ")
                ? refreshHeader.substring(7)
                : refreshHeader;

        // Refresh Token 유효성 검증
        jwtService.validateToken(refreshToken);

        // userId 추출 후 새 Access Token 발급
        Long userId = jwtService.extractUserId(refreshToken);
        String newAccessToken = jwtService.generateAccessToken(userId);

        return new AuthResponseDto.TokenReissueResponse(newAccessToken);
    }

	/**
	 * 테스트용 / sdk 환경에 사용할 용도로 카카오 엑세스 토큰을 받아 내부 로그인 처리를 하는 메소드 입니다.
	 * 위의 socialLogin 메소드에서 socialType을 kakao로 고정하고, 카카오 엑세스 토큰을 발급받는 과정을 제외하고 동일합니다.
	 *
	 * @param kakaoAccessToken 카카오 sdk 통해서 받아온 엑세스 토큰
	 * @return 토큰, userId 등 필요 정보
	 */
	public AuthResponseDto.LoginResponse kakaoLogin(String kakaoAccessToken) {

		try {
			// 카카오 유저 정보 조회
			KakaoUserResponse kakaoUser = kakaoAuthService.fetchUser(kakaoAccessToken);

			// DB에 유저 upsert
			User user = socialUserService.upsertKakaoUser(kakaoUser);

			// JWT 생성
			String accessToken = jwtService.generateAccessToken(user.getId());
			String refreshToken = jwtService.generateRefreshToken(user.getId());

			// 다음단계 게산
			String nextStep = user.determineNextStep();

			return new AuthResponseDto.LoginResponse(
				user.getId(),
				accessToken,
				refreshToken,
				user.getNickname(),
				user.getLoginStatus(),
				nextStep
			);
		} catch (GlobalException e) {
			// KakaoAuthService 내부에서 GlobalException 이미 던지면 그대로 전달
			throw e;
		} catch (Exception e) {
			// 외부 카카오 서버 통신 오류 처리
			throw new GlobalException(ErrorCode.AUTH_EXTERNAL_API_ERROR);
		}
	}
}
