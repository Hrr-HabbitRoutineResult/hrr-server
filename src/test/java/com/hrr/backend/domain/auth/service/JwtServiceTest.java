package com.hrr.backend.domain.auth.service;

import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // test용 프로파일 사용
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.serverCommands().flushAll();
            return null;
        });
    }

    @Test
    @DisplayName("Access Token 생성 및 검증 성공")
    void generateAndValidateAccessToken() {
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);

        assertThat(accessToken).isNotNull();
        assertThat(jwtService.validateToken(accessToken)).isTrue();
        assertThat(jwtService.extractUserId(accessToken)).isEqualTo(TEST_USER_ID);
    }

    @Test
    @DisplayName("Refresh Token 생성 시 Redis에 저장됨")
    void generateRefreshTokenSavesToRedis() {
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID);

        assertThat(refreshToken).isNotNull();

        String storedToken = redisTemplate.opsForValue().get("refresh_token:" + TEST_USER_ID);
        assertThat(storedToken).isEqualTo(refreshToken);
    }

    // [수정됨] validateRefreshToken 메서드 삭제로 인해 getAndDeleteRefreshToken 테스트로 변경
    @Test
    @DisplayName("Refresh Token 조회 및 삭제(Atomic) 성공")
    void getAndDeleteRefreshTokenSuccess() {
        // given
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID);

        // when
        // 조회와 동시에 삭제가 일어나는지 검증
        String storedToken = jwtService.getAndDeleteRefreshToken(TEST_USER_ID);

        // then
        assertThat(storedToken).isEqualTo(refreshToken); // 값 일치 확인
        assertThat(redisTemplate.hasKey("refresh_token:" + TEST_USER_ID)).isFalse(); // Redis에서 삭제되었는지 확인
    }

    @Test
    @DisplayName("토큰 블랙리스트 등록 및 확인")
    void blacklistToken() {
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);
        Duration ttl = Duration.ofMinutes(5);

        jwtService.blacklistToken(accessToken, ttl);

        assertThat(jwtService.isTokenBlacklisted(accessToken)).isTrue();
    }

    @Test
    @DisplayName("블랙리스트에 등록된 토큰은 검증 시 무효 처리")
    void blacklistedTokenIsInvalid() {
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);
        jwtService.blacklistToken(accessToken, Duration.ofMinutes(5));

        assertThatThrownBy(() -> jwtService.validateToken(accessToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_INVALID_TOKEN);
    }

    @Test
    @DisplayName("Refresh Token 삭제 성공")
    void deleteRefreshToken() {
        jwtService.generateRefreshToken(TEST_USER_ID);
        assertThat(redisTemplate.hasKey("refresh_token:" + TEST_USER_ID)).isTrue();

        jwtService.deleteRefreshToken(TEST_USER_ID);

        assertThat(redisTemplate.hasKey("refresh_token:" + TEST_USER_ID)).isFalse();
    }

    @Test
    @DisplayName("유효하지 않은 토큰 검증 시 예외 발생")
    void validateInvalidToken() {
        String invalidToken = "invalid.token.here";

        assertThatThrownBy(() -> jwtService.validateToken(invalidToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_INVALID_TOKEN);
    }

    @Test
    @DisplayName("남은 만료 시간 계산 정확성")
    void getRemainingExpiration() {
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);

        Duration remaining = jwtService.getRemainingExpiration(accessToken);

        assertThat(remaining.toMillis()).isGreaterThan(0);

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
    }
}