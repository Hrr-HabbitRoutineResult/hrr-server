package com.hrr.backend.domain.auth.service;

import com.hrr.backend.domain.auth.dto.AuthResponseDto;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        redisTemplate.keys("*").forEach(key -> redisTemplate.delete(key));
    }

    @Test
    @DisplayName("토큰 재발급 성공 - 새로운 AT와 RT 모두 발급됨")
    void reissueTokenSuccess() {
        // given
        String oldRefreshToken = jwtService.generateRefreshToken(TEST_USER_ID);
        String refreshHeader = "Bearer " + oldRefreshToken;

        // when
        AuthResponseDto.TokenReissueResponse response = authService.reissueToken(refreshHeader);

        // then
        assertThat(response.accessToken()).isNotNull();
        assertThat(response.refreshToken()).isNotNull();
        assertThat(response.refreshToken()).isNotEqualTo(oldRefreshToken);
    }

    @Test
    @DisplayName("토큰 재발급 시 기존 RT는 블랙리스트 처리됨")
    void reissueTokenBlacklistsOldRefreshToken() {
        // given
        String oldRefreshToken = jwtService.generateRefreshToken(TEST_USER_ID);
        String refreshHeader = "Bearer " + oldRefreshToken;

        // when
        authService.reissueToken(refreshHeader);

        // then
        assertThat(jwtService.isTokenBlacklisted(oldRefreshToken)).isTrue();
    }

    @Test
    @DisplayName("토큰 재발급 시 기존 RT로 재검증 시도하면 실패")
    void cannotReuseOldRefreshToken() {
        // given
        String oldRefreshToken = jwtService.generateRefreshToken(TEST_USER_ID);

        // when
        authService.reissueToken("Bearer " + oldRefreshToken);

        // then - 기존 RT로 다시 재발급 시도하면 실패
        assertThatThrownBy(() -> authService.reissueToken("Bearer " + oldRefreshToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_INVALID_TOKEN);
    }

    @Test
    @DisplayName("유효하지 않은 RT로 재발급 시도 시 예외 발생")
    void reissueTokenWithInvalidRefreshToken() {
        // given
        String invalidToken = "invalid.refresh.token";

        // when & then
        assertThatThrownBy(() -> authService.reissueToken("Bearer " + invalidToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_INVALID_TOKEN);
    }

    @Test
    @DisplayName("로그아웃 시 AT 블랙리스트 처리 및 RT 삭제")
    void logoutBlacklistsTokenAndDeletesRefreshToken() {
        // given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID);

        assertThat(redisTemplate.hasKey("refresh_token:" + TEST_USER_ID)).isTrue();

        // when
        authService.logout("Bearer " + accessToken);

        // then
        assertThat(jwtService.isTokenBlacklisted(accessToken)).isTrue();
        assertThat(redisTemplate.hasKey("refresh_token:" + TEST_USER_ID)).isFalse();
    }

    @Test
    @DisplayName("로그아웃 후 블랙리스트된 AT로 요청 시 실패해야 함")
    void cannotUseBlacklistedAccessToken() {
        // given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);

        // when
        authService.logout("Bearer " + accessToken);

        // then
        assertThat(jwtService.isTokenBlacklisted(accessToken)).isTrue();
    }

    @Test
    @DisplayName("로그아웃 후 RT로 재발급 시도 시 실패")
    void cannotReissueAfterLogout() {
        // given
        String accessToken = jwtService.generateAccessToken(TEST_USER_ID);
        String refreshToken = jwtService.generateRefreshToken(TEST_USER_ID);

        // when
        authService.logout("Bearer " + accessToken);

        // then - RT가 삭제되었으므로 재발급 실패
        assertThatThrownBy(() -> authService.reissueToken("Bearer " + refreshToken))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_INVALID_TOKEN);
    }
}