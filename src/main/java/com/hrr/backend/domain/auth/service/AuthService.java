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
            log.error("[socialLogin] Kakao 로그인 중 예상하지 못한 오류가 발생했습니다.", e);
            throw new GlobalException(ErrorCode.AUTH_EXTERNAL_API_ERROR, e);
        }
    }

    /**
     * Refresh Token 기반 Access Token 재발급
     * RT를 Redis에 저장하고, 재발급 시 전달받은 RT와 Redis 저장값을 비교하여 검증 강화
     * - validateToken()으로 서명/만료/블랙리스트 통합 검증
     * - Redis에서 RT를 원자적으로 조회+삭제(getAndDeleteRefreshToken) 후 요청값과 비교
     * - 검증 통과 시에만 새 AT/RT 발급, 기존 RT는 블랙리스트 처리
     */
    public AuthResponseDto.TokenReissueResponse reissueToken(String refreshHeader) {
        // "Bearer " 접두사 제거
        String refreshToken = refreshHeader.startsWith("Bearer ")
                ? refreshHeader.substring(7)
                : refreshHeader;

        // Refresh Token 유효성 검증
        jwtService.validateToken(refreshToken);

        // Redis에 저장된 Refresh Token과 비교 검증
        if (jwtService.isTokenBlacklisted(refreshToken)) {
            throw new GlobalException(ErrorCode.AUTH_INVALID_TOKEN);
        }

        // userId 추출 후 새 Access Token 발급
        Long userId = jwtService.extractUserId(refreshToken);

        String storedRefreshToken = jwtService.getAndDeleteRefreshToken(userId);

        // 저장된 토큰이 없거나(이미 사용됨/만료됨), 요청한 토큰과 다를 경우 실패 처리
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
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
            log.error("[kakaoLogin] Kakao SDK 로그인 중 예상하지 못한 오류가 발생했습니다.", e);
            throw new GlobalException(ErrorCode.AUTH_EXTERNAL_API_ERROR, e);
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
            log.error("[appleLogin] Apple 로그인 중 예상하지 못한 오류가 발생했습니다.", e);
            throw new GlobalException(ErrorCode.AUTH_EXTERNAL_API_ERROR, e);
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
            log.error("[naverLogin] Naver 로그인 중 예상하지 못한 오류가 발생했습니다.", e);
            throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR, e);
        }
    }

    /** 로그아웃 */
    public void logout(String tokenHeader) {

        // "Bearer " 접두사 제거
        String token = tokenHeader.startsWith("Bearer ")
                ? tokenHeader.substring(7)
                : tokenHeader;

        // userId 추출
        Long userId = jwtService.getUserIdFromToken(token);

        // 토큰의 남은 유효 기간 계산 (Duration 타입)
        // JWT의 exp 클레임과 현재 시간을 비교하여 남은 시간을 계산
        Duration remainingExpiration = jwtService.getRemainingExpiration(token);
        if (!remainingExpiration.isNegative() && !remainingExpiration.isZero()) {
            jwtService.blacklistToken(token, remainingExpiration);
        }

        // Refresh Token 삭제 (Redis에서 제거)
        jwtService.deleteRefreshToken(userId);
    }

    /**
     * 회원 탈퇴
     * 토큰 subject에서 userId 추출
     */
    @Transactional
    public void withdraw(String tokenHeader) {

        // AT 유효성 검증: 만료/블랙리스트/서명 이상 시 예외 발생
        String token = tokenHeader.startsWith("Bearer ") ? tokenHeader.substring(7) : tokenHeader;
        jwtService.validateToken(token);

        // 파라미터 userId 대신 토큰 subject에서 직접 userId 추출
        // → 컨트롤러에서 넘어온 파라미터를 맹목적으로 신뢰하지 않고, 토큰 주체를 직접 신뢰
        Long userId = jwtService.extractUserId(token);

        // 현재 트랜잭션 안에서 유저를 다시 조회 (영속화)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        // 유저 상태 변경 (Soft Delete)
        user.withdraw();

        // 챌린지 참여 상태 즉시 변경 로직 제거
        // → 탈퇴는 비활성화와 동일한 개념이므로 30일 유예기간 동안 재활성화 가능
        //   유예기간 중 챌린지 상태를 유지해야 재활성화 시 정상 복구됨
        //   즉시 탈퇴 처리로 발생한 공석을 타 유저가 선점할 경우,
        //   서비스 복귀 시 정원 초과로 인한 복구 실패를 방지하기 위함

        // AT 블랙리스트 처리 (남은 유효기간 동안 재사용 방지)
        Duration remainingExpiration = jwtService.getRemainingExpiration(token);
        if (!remainingExpiration.isNegative() && !remainingExpiration.isZero()) {
            jwtService.blacklistToken(token, remainingExpiration);
        }

        // 탈퇴 시 Refresh Token 삭제
        jwtService.deleteRefreshToken(userId);
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

        try {
            switch (socialAuth.getSocialType()) {
                case NAVER -> naverAuthService.revoke(socialAuth.getSocialRefreshToken());
                case APPLE -> appleAuthService.revoke(socialAuth.getSocialRefreshToken());
                case KAKAO -> kakaoAuthService.unlink(socialAuth.getSocialId());
                default -> {
                    log.error("[revoke] 지원하지 않는 socialType입니다. userId={}, socialType={}",
                            userId, socialAuth.getSocialType());
                    throw new GlobalException(ErrorCode.AUTH_INVALID_SOCIAL_TYPE);
                }
            }
        } catch (GlobalException e) {
            // 하위 소셜 서비스가 원인을 이미 정확한 위치에서 기록한다. 여기서 다시 기록하면
            // 하나의 연동 실패가 Discord에 두 번 전송되므로 오류 코드만 보존해 전달한다.
            throw e;
        } catch (Exception e) {
            log.error("[revoke] 소셜 연결 해제 중 예상하지 못한 오류가 발생했습니다. userId={}", userId, e);
            throw new GlobalException(ErrorCode.AUTH_EXTERNAL_API_ERROR, e);
        } finally {
            // 외부 연동 해제 실패 여부와 상관없이 내부 Refresh Token은 삭제하여 재로그인 방지
            jwtService.deleteRefreshToken(userId);
        }
}
}
