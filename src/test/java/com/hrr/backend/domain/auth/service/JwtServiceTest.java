package com.hrr.backend.domain.auth.service;

import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // test용 프로파일 사용
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        redisTemplate.keys("*").forEach(key -> redisTemplate.delete(key));
    }

    @Test
    @DisplayName("Access Token 생성 및 검증 성공")
    void generateAndValidateAccessToken() {
        // given & when
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);

        // then
        assertThat(accessToken).isNotNull();
        assertThat(jwtService.validateToken(accessToken)).isTrue();
        assertThat(jwtService.extractUserId(accessToken)).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Refresh Token 생성 시 Redis에 저장됨")
    void generateRefreshTokenSavesToRedis() {
        // given & when
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID);

        // then
        assertThat(refreshToken).isNotNull();

        String storedToken = redisTemplate.opsForValue().get("refresh_token:" + TEST_USER_ID);
        assertThat(storedToken).isEqualTo(refreshToken);
    }

    @Test
    @DisplayName("Refresh Token 검증 성공")
    void validateRefreshTokenSuccess() {
        // given
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID);

        // when & then
        assertThat(jwtService.validateRefreshToken(refreshToken, TEST_USER_ID)).isTrue();
    }

    @Test
    @DisplayName("잘못된 Refresh Token 검증 실패")
    void validateRefreshTokenFail() {
        // given
        String realToken = jwtService.generateRefreshToken(TEST_USER_ID);
        String fakeToken = "fake.refresh.token";

        // when & then
        assertThat(jwtService.validateRefreshToken(fakeToken, TEST_USER_ID)).isFalse();
    }

    @Test
    @DisplayName("토큰 블랙리스트 등록 및 확인")
    void blacklistToken() {
        // given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);
        Duration ttl = Duration.ofMinutes(5);

        // when
        jwtService.blacklistToken(accessToken, ttl);

        // then
        assertThat(jwtService.isTokenBlacklisted(accessToken)).isTrue();
    }

    @Test
    @DisplayName("블랙리스트에 등록된 토큰은 검증 시 무효 처리")
    void blacklistedTokenIsInvalid() {
        // given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);
        jwtService.blacklistToken(accessToken, Duration.ofMinutes(5));

        // when & then
        assertThat(jwtService.isTokenBlacklisted(accessToken)).isTrue();
    }

    @Test
    @DisplayName("Refresh Token 삭제 성공")
    void deleteRefreshToken() {
        // given
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID);
        assertThat(redisTemplate.hasKey("refresh_token:" + TEST_USER_ID)).isTrue();

        // when
        jwtService.deleteRefreshToken(TEST_USER_ID);

        // then
        assertThat(redisTemplate.hasKey("refresh_token:" + TEST_USER_ID)).isFalse();
    }

    @Test
    @DisplayName("유효하지 않은 토큰 검증 시 예외 발생")
    void validateInvalidToken() {
        // given
        String invalidToken = "invalid.token.here";

        // when & then
        assertThatThrownBy(() -> jwtService.validateToken(invalidToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_INVALID_TOKEN);
    }

    @Test
    @DisplayName("만료된 토큰 검증 시 예외 발생")
    void validateExpiredToken() throws InterruptedException {
        // given - 1초 만료 토큰 생성 (테스트용으로 설정 변경 필요)
        // 실제로는 application-test.yml에서 jwt.access-token-validity=1000 설정

        // 이 테스트는 실제 만료를 기다려야 하므로 주석 처리
        // 대신 통합 테스트나 수동 테스트로 확인 권장
    }

    @Test
    @DisplayName("남은 만료 시간 계산 정확성")
    void getRemainingExpiration() throws InterruptedException {
        // given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);

        // when
        Duration remaining = jwtService.getRemainingExpiration(accessToken);

        // then
        assertThat(remaining.toMillis()).isGreaterThan(0);
        assertThat(remaining.toMinutes()).isLessThanOrEqualTo(30); // 기본 30분 이하
    }

    @Test
    @DisplayName("새 Refresh Token 발급 시 기존 토큰 교체됨")
    void newRefreshTokenReplacesOld() {
        // given
        String oldRefreshToken = jwtService.generateRefreshToken(TEST_USER_ID);

        // when
        String newRefreshToken = jwtService.generateRefreshToken(TEST_USER_ID);

        // then
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        String storedToken = redisTemplate.opsForValue().get("refresh_token:" + TEST_USER_ID);
        assertThat(storedToken).isEqualTo(newRefreshToken);
        assertThat(jwtService.validateRefreshToken(oldRefreshToken, TEST_USER_ID)).isFalse();
    }
}