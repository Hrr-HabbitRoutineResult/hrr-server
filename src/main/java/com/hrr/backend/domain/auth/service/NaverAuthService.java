package com.hrr.backend.domain.auth.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.hrr.backend.domain.auth.dto.NaverUserResponse;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverAuthService {

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

}
