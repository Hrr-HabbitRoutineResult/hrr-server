package com.hrr.backend.domain.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.security.PrivateKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	private final RestTemplate restTemplate;
	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Identity Token에서 유저 식별자(sub) 추출 (단순 파싱 버전; 추후 검증 로직 추가 예정)
	 */
	public String getAppleAccountId(String identityToken) {
		try {
			// JWT: [header].[payload].[signature] 구조
			String[] chunks = identityToken.split("\\.");
			if (chunks.length < 2) {
				throw new IllegalArgumentException("Invalid JWT token");
			}

			// Payload 디코딩; URL Safe Base64 디코더를 사용
			Base64.Decoder decoder = Base64.getUrlDecoder();
			String payload = new String(decoder.decode(chunks[1]));

			// ObjectMapper를 사용하여 JSON에서 sub 필드만 추출
			JsonNode jsonNode = objectMapper.readTree(payload);

			// "sub" 필드가 null 체크
			if (jsonNode.get("sub") == null || jsonNode.get("sub").isNull() || jsonNode.get("sub").asText().isBlank()) {
				log.warn("[getAppleAccountId] Apple id_token에 sub 필드가 없습니다.");
				throw new GlobalException(ErrorCode.AUTH_APPLE_ID_TOKEN_INVALID);
			}

			String sub = jsonNode.get("sub").asText();

			return sub;
		} catch (GlobalException e) {
			throw e;
		} catch (Exception e) {
			log.warn("[getAppleAccountId] Apple id_token 파싱에 실패했습니다. exception={}",
				e.getClass().getSimpleName());
			throw new GlobalException(ErrorCode.AUTH_APPLE_ID_TOKEN_INVALID);
		}
	}

	/**
	 * Authorization Code로 애플 id_token과 Refresh Token 받아오기
	 */
	public Map<String, String> getAppleTokens(String code) {
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
			ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
				tokenUrl,
				HttpMethod.POST,
				entity,
				new ParameterizedTypeReference<Map<String, Object>>() {}
			);

			Map<String, Object> body = response.getBody();

			// 응답 바디 전체가 null 인지 확인
			if (body == null) {
				log.warn("[getAppleTokens] Apple token API 응답 body가 비어 있습니다.");
				throw new GlobalException(ErrorCode.AUTH_APPLE_TOKEN_ERROR);
			}

			// 애플 측에서 보낸 에러 메시지가 있는지 확인 (디버깅용)
			if (body.containsKey("error")) {
				log.warn("[getAppleTokens] Apple 인증 요청이 거부되었습니다. error={}", body.get("error"));
				throw new GlobalException(ErrorCode.AUTH_APPLE_TOKEN_ERROR);
			}

			String idToken = (String) body.get("id_token");
			String refreshToken = (String) body.get("refresh_token");

			// StringUtils 를 활용하여 null 및 빈 문자열 (""), 공백 (" ") 체크
			if (!StringUtils.hasText(idToken) ||
				!StringUtils.hasText(refreshToken)) {
				log.warn("[getAppleTokens] Apple token 응답에 필수 필드가 없습니다. idTokenPresent={}, refreshTokenPresent={}",
					StringUtils.hasText(idToken), StringUtils.hasText(refreshToken));
				throw new GlobalException(ErrorCode.AUTH_APPLE_TOKEN_ERROR);
			}

			return Map.of("id_token", idToken, "refresh_token", refreshToken);

		} catch (HttpClientErrorException e) {
			log.warn("[getAppleTokens] Apple token API 요청이 거부되었습니다. status={}", e.getStatusCode());
			throw new GlobalException(ErrorCode.AUTH_APPLE_TOKEN_ERROR);
		} catch (GlobalException e) {
			throw e;
		} catch (Exception e) {
			log.error("[getAppleTokens] Apple 토큰 처리 중 예상하지 못한 오류가 발생했습니다.", e);
			throw new GlobalException(ErrorCode.AUTH_APPLE_TOKEN_ERROR, e);
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
			// P8 키 형식이 깨졌거나 설정이 잘못된 경우 - 로그 없이 던지면 원인 파악이 불가능해 반드시 남긴다
			log.error("[getPrivateKey] Apple P8 private key 파싱에 실패했습니다.", e);
			throw new GlobalException(ErrorCode.AUTH_APPLE_KEY_ERROR, e);
		}
	}

	/**
	 * 애플 연결 해제
	 */
	public void revoke(String appleRefreshToken) {
		String revokeUrl = "https://appleid.apple.com/auth/revoke";
		String clientSecret = createClientSecret();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("client_id", CLIENT_ID);
		params.add("client_secret", clientSecret);
		params.add("token", appleRefreshToken); // socialAuth에 저장해둔 리프레시 토큰
		params.add("token_type_hint", "refresh_token");

		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

		try {
			ResponseEntity<String> response = restTemplate.postForEntity(revokeUrl, entity, String.class);

			if (!response.getStatusCode().is2xxSuccessful()) {
				log.error("[revoke] Apple 연결 해제에 실패했습니다. status={}", response.getStatusCode());
				throw new GlobalException(ErrorCode.AUTH_APPLE_REVOKE_ERROR);
			}
			log.info("[revoke] Apple 연결 해제를 완료했습니다.");
		} catch (HttpClientErrorException e) {
			log.warn("[revoke] Apple 연결 해제 요청이 거부되었습니다. status={}", e.getStatusCode());
			throw new GlobalException(ErrorCode.AUTH_APPLE_REVOKE_ERROR);
		} catch (GlobalException e) {
			// 비정상 응답 분기에서 이미 한 번 기록했다.
			throw e;
		} catch (Exception e) {
			log.error("[revoke] Apple revoke API 통신 중 오류가 발생했습니다.", e);
			throw new GlobalException(ErrorCode.AUTH_APPLE_REVOKE_ERROR, e);
		}
	}
}
