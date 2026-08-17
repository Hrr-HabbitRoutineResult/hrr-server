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
			throw new GlobalException(ErrorCode.AUTH_NAVER_TOKEN_INVALID);
		} catch (HttpClientErrorException e) {
			throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
		} catch (Exception e) {
			// 그 외 통신 장애 등 모든 외부 API 오류
			log.error("[fetchUser] Naver 사용자 정보 API 호출 중 예상하지 못한 오류가 발생했습니다.", e);
			throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR, e);
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
				log.error("[revoke] Naver 연결 해제에 실패했습니다. status={}", response.getStatusCode());
				throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
			}

			NaverResponseDto.NaverRevokeResponse body = response.getBody();

			// 응답 바디 및 결과값 방어적 체크
			if (body == null) {
				log.error("[revoke] Naver 연결 해제 API 응답 body가 비어 있습니다.");
				throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
			}

			// 네이버가 보낸 result 값 검증
			if ("success".equals(body.getResult())) {
				log.info("[revoke] Naver 연결 해제를 완료했습니다.");
				return true;
			} else {
				// 네이버가 success 가 아닌 에러 코드를 보낸 경우 (이미 해제되었거나 토큰이 잘못된 경우 등)
				throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
			}

		} catch (HttpClientErrorException e) {
			// 만료되었거나 이미 해제된 token 등 예상 가능한 4xx는 호출자 응답으로 충분하다.
			throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
		} catch (RestClientException e) {
			// 주의: revokeUrl에 client_secret/access_token이 쿼리 파라미터로 포함되어 있고,
			// RestClientException(예: ResourceAccessException)의 메시지에는 실패한 요청 URL 전체가 그대로 담긴다.
			// e를 그대로 로깅하거나 cause로 체이닝하면 스택트레이스를 통해 시크릿이 콘솔/Discord로 노출되므로
			// 절대 e를 로깅/체이닝하지 않고 예외 타입만 안전하게 남긴다.
			log.error("[revoke] Naver 연결 해제 API 통신 중 오류가 발생했습니다. exception={}",
				e.getClass().getSimpleName());
			throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
		} catch (GlobalException e) {
			// 응답 검증 분기에서 이미 한 번 기록했으므로 상위 계층에서 다시 알리지 않는다.
			throw e;
		} catch (Exception e) {
			// 그 외 예상치 못한 런타임 에러 - 위와 동일한 이유로 e를 로깅/체이닝하지 않음
			log.error("[revoke] Naver 연결 해제 중 예상하지 못한 오류가 발생했습니다. exception={}",
				e.getClass().getSimpleName());
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

		try {
			ResponseEntity<NaverResponseDto.NaverTokenResponse> response = restTemplate.getForEntity(refreshUrl, NaverResponseDto.NaverTokenResponse.class);

			if (response.getBody() != null && response.getBody().getAccessToken() != null) {
				return response.getBody().getAccessToken();
			}
		} catch (HttpClientErrorException e) {
			// 만료되었거나 유효하지 않은 refresh token은 정상적인 인증 실패로 처리한다.
			throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
		} catch (Exception e) {
				// refreshUrl에 client_secret/refresh_token이 쿼리 파라미터로 포함되어 있어, e를 그대로 로깅하면
				// 예외 메시지에 담긴 요청 URL을 통해 시크릿이 노출된다 - 예외 타입만 안전하게 남긴다.
				log.error("[refreshNaverToken] Naver token 갱신에 실패했습니다. exception={}",
					e.getClass().getSimpleName());
		}

		throw new GlobalException(ErrorCode.AUTH_NAVER_EXTERNAL_ERROR);
	}

}
