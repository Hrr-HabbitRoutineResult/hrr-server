package com.hrr.backend.domain.auth.service;

import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    //액세스 토큰 만료시간
    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    // 리프레시 토큰 만료시간
    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "token_blacklist:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:"; // 추가

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Access Token 생성 */
    public String generateAccessToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenValidity))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Refresh Token 생성
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        String refreshToken = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshTokenValidity))
                .setId(java.util.UUID.randomUUID().toString())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        // Refresh Token을 Redis에 저장 (userId를 키로 사용)
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + userId,
                refreshToken,
                Duration.ofMillis(refreshTokenValidity)
        );

        return refreshToken;
    }

    /**
     * 토큰 유효성 검증 (서명 및  만료 여부 확인 위함)
     * 유효하면 true 반환하도록 진행*/
    public boolean validateToken(String token) {
        if (isTokenBlacklisted(token)) {
            throw new GlobalException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw new GlobalException(ErrorCode.AUTH_TOKEN_EXPIRED);
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | IllegalArgumentException e) {
            throw new GlobalException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    /** 토큰 만료 여부 확인
     * 만료가 되면 true를 반환하게 하고 유효하거나 파싱 불가할 때 false 반환하게 함*/
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /** userId(subject) 추출 */
    public Long extractUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.parseLong(claims.getSubject());
        } catch (JwtException e) {
            throw new GlobalException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Long.parseLong(claims.getSubject());
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서도 Claims(Subject=UserId) 추출 가능
            return Long.parseLong(e.getClaims().getSubject());
        } catch (Exception e) {
            // 그 외 서명 불일치 등은 유효하지 않은 토큰 처리
            throw new GlobalException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    /** HTTP 요청 헤더에서 JWT 추출
     * Authroization: Bearer {token} 형태에서 token 만 분리하는 것임*/
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 토큰을 더 이상 사용하지 못 하게 무효화
    public void blacklistToken(String token, Duration expirationDuration) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token,
                "invalidated",
                expirationDuration
        );
    }

    // 블랙리스트 포함 여부 조회
    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    /**
     * JWT 토큰에서 남은 유효 기간(TTL)을 계산하여 반환
     * * @param token JWT 문자열 (Bearer 접두사가 없는 순수 토큰)
     * @return 토큰의 남은 유효 기간 (Duration). 이미 만료되었거나 파싱 오류 시 Duration.ZERO 반환.
     */
    public Duration getRemainingExpiration(String token) {
        try {
            // 토큰 파싱 및 Claims 획득
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey()) // 토큰 검증에 사용된 시크릿 키
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 만료 시간 (exp 클레임) 획득
            Date expiration = claims.getExpiration();
            Date now = new Date();

            // 남은 시간 계산
            if (expiration.after(now)) {
                // 만료 시간이 현재 시간보다 늦다면, 남은 밀리초를 계산
                long diffInMillis = expiration.getTime() - now.getTime();
                return Duration.ofMillis(diffInMillis);
            } else {
                // 이미 만료된 토큰인 경우
                return Duration.ZERO;
            }

        } catch (Exception e) {
            return Duration.ZERO;
        }
    }

    // Redis에서 키를 조회함과 동시에 삭제하여 동시성 문제를 해결
    public String getAndDeleteRefreshToken(Long userId) {
        return redisTemplate.opsForValue().getAndDelete(REFRESH_TOKEN_PREFIX + userId);
    }

    // Refresh Token 삭제 (로그아웃 시 사용)
    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + userId);
    }
}