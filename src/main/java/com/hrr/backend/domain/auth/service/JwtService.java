package com.hrr.backend.domain.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;
    /*
    @Value("${jwt.access-token-validity}")
    private long accessValidity; // ms
     */

    //액세스 토큰 만료시간 (예: 30분)
    private final long ACCESS_TOKEN_VALIDITY = 1000L * 60 * 30;
    // 리프레시 토큰 만료시간 (예: 7일)
    private final long REFRESH_TOKEN_VALIDITY = 1000L * 60 * 60 * 24 * 7;

    public String generateAccessToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_VALIDITY))
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }
    /**
     * Refresh Token 생성
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_VALIDITY))
                .signWith(SignatureAlgorithm.HS256, secret.getBytes())
                .compact();
    }

}
