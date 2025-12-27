package com.hrr.backend.domain.auth.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.hrr.backend.domain.auth.dto.NaverResponseDto;
import com.hrr.backend.domain.auth.dto.NaverUserResponse;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverAuthService {

	@Value("${naver.client-id}")
	private String clientId;

	@Value("${naver.client-secret}")
	private String clientSecret;

	private final RestTemplate restTemplate;
	private static final String NAVER_USER_INFO_URL = "https://openapi.naver.com/v1/nid/me";

	public NaverUserResponse fetchUser(String accessToken) {
		// 헤더 설정 (Authorization: Bearer {AT})
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<String> entity = new HttpEntity<>(headers);

		try {
			// 네이버 프로필 API 호출
			ResponseEntity<NaverUserResponse> response = restTemplate.exchange(
				NAVER_USER_INFO_URL,
				HttpMethod.GET,
				entity,
				NaverUserResponse.class
			);

			if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
				return response.getBody();
			} else {
				throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
			}
		} catch (HttpClientErrorException.Unauthorized e) {
			// 401 Unauthorized 에러 발생 시 (토큰 만료 등)
			log.error("네이버 토큰이 유효하지 않음: {}", e.getMessage());
			throw new GlobalException(ErrorCode.AUTH_NAVER_TOKEN_INVALID);
		} catch (Exception e) {
			// 그 외 통신 장애 등 모든 외부 API 오류
			log.error("네이버 API 호출 중 예상치 못한 오류 발생: {}", e.getMessage());
			throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
		}
	}

	/**
	 * 네이버 연동 해제
	 */
	public boolean revoke(String refreshToken) {
		// 저장된 RT를 사용해 새로운 AT를 먼저 발급
		String accessToken = refreshNaverToken(refreshToken);

		// 연동 해제 API 호출 URL 생성
		String revokeUrl = UriComponentsBuilder.fromUriString("https://nid.naver.com/oauth2.0/token")
			.queryParam("grant_type", "delete")
			.queryParam("client_id", clientId)
			.queryParam("client_secret", clientSecret)
			.queryParam("access_token", accessToken)
			.queryParam("service_provider", "NAVER")
			.toUriString();

		try {
			// 네이버 연동 해제 API 호출
			ResponseEntity<NaverResponseDto.NaverRevokeResponse> response =
				restTemplate.getForEntity(revokeUrl, NaverResponseDto.NaverRevokeResponse.class);

			// HTTP 상태 코드 확인 (200 OK 인지)
			if (!response.getStatusCode().is2xxSuccessful()) {
				log.error("네이버 연동 해제 실패 (HTTP Status): {}", response.getStatusCode());
				throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
			}

			NaverResponseDto.NaverRevokeResponse body = response.getBody();

			// 응답 바디 및 결과값 방어적 체크
			if (body == null) {
				log.error("네이버 연동 해제 실패: 응답 바디가 비어 있습니다.");
				throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
			}

			// 네이버가 보낸 result 값 검증
			if ("success".equals(body.getResult())) {
				log.info("네이버 연동 해제 성공 ");
				return true;
			} else {
				// 네이버가 success 가 아닌 에러 코드를 보낸 경우 (이미 해제되었거나 토큰이 잘못된 경우 등)
				log.error("네이버 연동 해제 거부 : error={}, description={}",
					body.getError(), body.getErrorDescription());

				throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
			}

		} catch (RestClientException e) {
			// RestTemplate 통신 자체에서 발생한 에러 (타임아웃 , 연결 실패 등)
			log.error("네이버 서버와 통신 중 네트워크 에러 발생 : ", e);
			throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
		} catch (Exception e) {
			// 그 외 예상치 못한 런타임 에러
			log.error("네이버 연동 해제 중 알 수 없는 에러 발생 : ", e);
			throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
		}

	}

	/**
	 * RT로 새로운 AT 발급
	 */
	private String refreshNaverToken(String refreshToken) {
		String refreshUrl = UriComponentsBuilder.fromUriString("https://nid.naver.com/oauth2.0/token")
			.queryParam("grant_type", "refresh_token")
			.queryParam("client_id", clientId)
			.queryParam("client_secret", clientSecret)
			.queryParam("refresh_token", refreshToken)
			.toUriString();

		ResponseEntity<NaverResponseDto.NaverTokenResponse> response = restTemplate.getForEntity(refreshUrl, NaverResponseDto.NaverTokenResponse.class);

		if (response.getBody() != null && response.getBody().getAccessToken() != null) {
			return response.getBody().getAccessToken();
		}

		throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
	}

}
