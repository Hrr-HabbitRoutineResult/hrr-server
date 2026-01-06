package com.hrr.backend.domain.auth.service;

import java.time.Duration;
import java.util.List;
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
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
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
					user.getDisplayName(),
					user.getDisplayNickname(),
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
				user.getDisplayName(),
				user.getDisplayNickname(),
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

	/**
	 * 애플 로그인 구현
	 *
	 * @param request 요청 Dto
	 * @return 토큰, userId 등 필요 정보
	 */
	public AuthResponseDto.LoginResponse appleLogin(AuthRequestDto.AppleLoginRequest request) {

		try {
			Map<String, String> appleTokens = appleAuthService.getAppleTokens(request.getAuthorizationCode());

			// id_token 자체가 오지 않은 경우 처리
			if (appleTokens == null || appleTokens.get("id_token") == null) {
				log.error("애플 id_token이 유효하지 않습니다.");
				throw new GlobalException(ErrorCode.AUTH_APPLE_TOKEN_ERROR);
			}

			String socialId = appleAuthService.getAppleAccountId(appleTokens.get("id_token"));
			String appleRefreshToken = appleTokens.get("refresh_token");

			// DB 저장 (애플은 RT 함께 저장)
			User user = socialUserService.upsertAppleUser(socialId, appleRefreshToken, request.getName());

			// JWT 생성
			String accessToken = jwtService.generateAccessToken(user.getId());
			String refreshToken = jwtService.generateRefreshToken(user.getId());

			// 다음단계 게산
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
			// 애플 서버 통신 오류 처리

			throw new GlobalException(ErrorCode.AUTH_EXTERNAL_API_ERROR);
		}
	}

	/**
	 * 네이버 엑세스 토큰을 통해 로그인 (SDK 방식)
	 * @param naverAccessToken 네이버 sdk를 통해 프론트에서 받아온 엑세스 토큰
	 * @return 토큰, userId 등 필요 정보
	 */
	public AuthResponseDto.LoginResponse naverLogin(String naverAccessToken, String naverRefreshToken) {

		try {
			// 네이버 유저 정보 조회
			NaverUserResponse naverUser = naverAuthService.fetchUser(naverAccessToken);

			// DB에 유저 upsert
			User user = socialUserService.upsertNaverUser(naverUser, naverRefreshToken);

			// JWT 생성
			String accessToken = jwtService.generateAccessToken(user.getId());
			String refreshToken = jwtService.generateRefreshToken(user.getId());

			// 다음 단계 계산
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
			// 이미 NaverAuthService에서 던진 전용 에러를 그대로 위로 던짐
			throw e;
		} catch (Exception e) {
			// 그 외 서버 내부 로직 오류 시 공통 외부 에러 처리
			log.error("네이버 로그인 중 오류 발생: ", e);
			throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
		}
	}

	public void logout(String tokenHeader) {

		// "Bearer " 접두사 제거
		String token = tokenHeader.startsWith("Bearer ")
			? tokenHeader.substring(7)
			: tokenHeader;

		// 토큰의 남은 유효 기간 계산 (Duration 타입)
		// JWT의 exp 클레임과 현재 시간을 비교하여 남은 시간을 계산
		Duration remainingExpiration = jwtService.getRemainingExpiration(token);

		if (remainingExpiration.isNegative() || remainingExpiration.isZero()) {
			// 이미 만료된 토큰인 경우, 굳이 블랙리스트에 넣을 필요는 없어 패스
			return;
		}

		// 토큰을 블랙리스트에 등록 (TTL은 남은 유효 기간으로 설정)
		jwtService.blacklistToken(token, remainingExpiration);

	}

	/**
	 * 회원 탈퇴
	 * @param userId 탈퇴할 사용자의 userId
	 */
	@Transactional
	public void withdraw(Long userId) {
		// 현재 트랜잭션 안에서 유저를 다시 조회 (영속화)
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

		// 유저 상태 변경 (Soft Delete)
		user.withdraw();

		// 해당 사용자가 '참여 중(JOINED)'인 챌린지만 조회
		List<UserChallenge> activeParticipations =
				userChallengeRepository.findByUserAndStatus(user, ChallengeJoinStatus.JOINED);

		for (UserChallenge uc : activeParticipations) {
			// 연관된 챌린지의 현재 인원수 필드 감소
			uc.getChallenge().decreaseCurrentParticipants();

			// 유저의 참여 상태를 '하차(DROPPED)'로 업데이트
			uc.updateStatus(ChallengeJoinStatus.DROPPED);
		}

		// TODO: 리프레시 토큰 등 세션 정보 삭제
	}

	@Transactional
	// 소셜 로그인 연결 해제
	public void revoke(Long userId) {
		// 현재 트랜잭션 안에서 유저를 다시 조회 (영속화)
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

		// 소셜 연동 해제
		SocialAuth socialAuth = socialAuthRepository.findByUser(user)
			.orElseThrow(() -> new GlobalException(ErrorCode.AUTH_INFO_NOT_FOUND));

		switch (socialAuth.getSocialType()) {
			case NAVER -> naverAuthService.revoke(socialAuth.getSocialRefreshToken());
			case APPLE -> appleAuthService.revoke(socialAuth.getSocialRefreshToken());
			case KAKAO -> kakaoAuthService.unlink(socialAuth.getSocialId());
			default -> throw new GlobalException(ErrorCode.AUTH_INVALID_SOCIAL_TYPE);
		}
	}

}
