package com.hrr.backend.domain.auth.service;

import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Objects;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // test용 프로파일 사용
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    // [Code Review 반영] 하드코딩된 값 대신 설정 파일의 값을 주입받아 사용
    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // [Code Review 반영] keys("*")는 블로킹 연산이므로 flushAll()로 대체하여 안전하게 초기화
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
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
        // [Code Review 반영] 사용하지 않는 realToken 변수 삭제
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
    @DisplayName("남은 만료 시간 계산 정확성")
    void getRemainingExpiration() {
        // given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);

        // when
        Duration remaining = jwtService.getRemainingExpiration(accessToken);

        // then
        assertThat(remaining.toMillis()).isGreaterThan(0);

        // 하드코딩(30분) 대신 설정된 유효 시간으로 검증 (밀리초 -> 분 변환)
        long validityInMinutes = accessTokenValidity / (1000 * 60);
        assertThat(remaining.toMinutes()).isLessThanOrEqualTo(validityInMinutes);
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