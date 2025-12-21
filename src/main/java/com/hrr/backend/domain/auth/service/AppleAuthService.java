package com.hrr.backend.domain.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.security.PrivateKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleAuthService {

	@Value("${apple.team-id}") private String TEAM_ID;
	@Value("${apple.client-id}") private String CLIENT_ID;
	@Value("${apple.key-id}") private String KEY_ID;
	@Value("${apple.p8-key}") private String P8_KEY; // .p8 파일의 텍스트 내용 전체

	private final RestTemplate restTemplate = new RestTemplate();

	/**
	 * Identity Token에서 유저 식별자(sub) 추출 (단순 파싱 버전; 추후 검증 로직 추가 예정)
	 */
	public String getAppleAccountId(String identityToken) {
		try {
			String[] chunks = identityToken.split("\\.");
			Base64.Decoder decoder = Base64.getUrlDecoder();
			String payload = new String(decoder.decode(chunks[1]));

			return payload.substring(payload.indexOf("\"sub\":\"") + 7, payload.indexOf("\",\"auid\""));
		} catch (Exception e) {
			throw new GlobalException(ErrorCode.AUTH_APPLE_ID_TOKEN_INVALID);
		}
	}

	/**
	 * Authorization Code로 애플 Refresh Token 받아오기
	 */
	public String getAppleRefreshToken(String code) {
		String clientSecret = createClientSecret();
		String tokenUrl = "https://appleid.apple.com/auth/token";

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("client_id", CLIENT_ID);
		params.add("client_secret", clientSecret);
		params.add("code", code);
		params.add("grant_type", "authorization_code");

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, entity, Map.class);
			return (String) response.getBody().get("refresh_token");
		} catch (Exception e) {
			log.error("애플 RT 발급 실패");
			throw new GlobalException(ErrorCode.AUTH_APPLE_TOKEN_ERROR);
		}
	}

	/**
	 * 애플 서버 통신용 Client Secret(JWT) 생성
	 */
	private String createClientSecret() {
		Date now = new Date();
		Date expiration = new Date(now.getTime() + 3600000); // 1시간

		return Jwts.builder()
			.setHeaderParam("alg", "ES256")
			.setHeaderParam("kid", KEY_ID)
			.setIssuer(TEAM_ID)
			.setIssuedAt(now)
			.setExpiration(expiration)
			.setAudience("https://appleid.apple.com")
			.setSubject(CLIENT_ID)
			.signWith(getPrivateKey(), SignatureAlgorithm.ES256)
			.compact();
	}

	/**
	 * .p8 문자열을 PrivateKey 객체로 변환
	 */
	private PrivateKey getPrivateKey() {
		try {
			String privateKeyPEM = P8_KEY
				.replace("-----BEGIN PRIVATE KEY-----", "")
				.replace("-----END PRIVATE KEY-----", "")
				.replaceAll("\\s+", "");

			byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
			PrivateKeyInfo keyInfo = PrivateKeyInfo.getInstance(encoded);
			return new JcaPEMKeyConverter().getPrivateKey(keyInfo);
		} catch (Exception e) {
			throw new GlobalException(ErrorCode.AUTH_APPLE_KEY_ERROR);
		}
	}
}
