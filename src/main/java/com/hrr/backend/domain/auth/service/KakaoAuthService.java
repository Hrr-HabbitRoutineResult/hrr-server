package com.hrr.backend.domain.auth.service;

import com.hrr.backend.domain.auth.dto.KakaoTokenResponse;
import com.hrr.backend.domain.auth.dto.KakaoUserResponse;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class KakaoAuthService {
    private  final WebClient webClient;
    public KakaoAuthService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
		// 타임아웃 설정 (5초 연결, 5초 읽기/쓰기)
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(5))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.SECONDS)));

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://kauth.kakao.com")
                .build();
    }

    @Value("${kakao.client-id}")
    private String clientId;       // REST API 키

    @Value("${kakao.redirect-uri}")
    private String redirectUri;  // hrr://oauth/kakao

    @Value("${kakao.client-secret}")
    private String clientSecret;

	@Value("${kakao.admin-key}")
	private String adminKey;

	private final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

	private final RestTemplate restTemplate;

    /*카카오 인가코드로 토큰 발급*/
    public KakaoTokenResponse exchangeToken(String code) {
        try {
            return webClient.post()
                    .uri("/oauth/token")
                    .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                            .with("client_id", clientId)
                            .with("redirect_uri", redirectUri)
                            .with("code", code)
                            .with("client_secret", clientSecret))
                    .retrieve()
                    // 상태 코드별 에러 처리
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> {
                                        if (response.statusCode().is5xxServerError()) {
                                            log.error("[exchangeToken] Kakao token API 요청에 실패했습니다. status={}",
                                                    response.statusCode());
										} else {
											log.warn("[exchangeToken] Kakao token API 요청이 거부되었습니다. status={}",
												response.statusCode());
                                        }
                                        return new GlobalException(ErrorCode.AUTH_KAKAO_TOKEN_ERROR);
                                    }))
                    .bodyToMono(KakaoTokenResponse.class)
                    .block(Duration.ofSeconds(5)); // 블록에도 최대 대기시간 명시
        } catch (GlobalException e) {
            // onStatus에서 이미 한 번만 ERROR로 기록했다.
            throw e;
        } catch (Exception e) {
            // 타임아웃/연결거부 등 네트워크 레벨 실패 (위 onStatus 매퍼를 타지 않는 경우)
            log.error("[exchangeToken] Kakao token API 통신 중 오류가 발생했습니다.", e);
            throw new GlobalException(ErrorCode.AUTH_KAKAO_TOKEN_ERROR, e);
        }
    }

    /**
     * 카카오 사용자 정보 조회
     */
    public KakaoUserResponse fetchUser(String accessToken) {
        try {
            return webClient.get()
                    .uri("https://kapi.kakao.com/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                            response -> response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> {
                                        if (response.statusCode().is5xxServerError()) {
                                            log.error("[fetchUser] Kakao 사용자 정보 API 요청에 실패했습니다. status={}",
                                                    response.statusCode());
										} else {
											log.warn("[fetchUser] Kakao 사용자 정보 API 요청이 거부되었습니다. status={}",
												response.statusCode());
                                        }
                                        return new GlobalException(ErrorCode.AUTH_KAKAO_USER_ERROR);
                                    }))
                    .bodyToMono(KakaoUserResponse.class)
                    .block(Duration.ofSeconds(5));
        } catch (GlobalException e) {
            // onStatus에서 이미 한 번만 ERROR로 기록했다.
            throw e;
        } catch (Exception e) {
            // 타임아웃/연결거부 등 네트워크 레벨 실패 (위 onStatus 매퍼를 타지 않는 경우)
            // NaverAuthService에도 동일한 이름의 fetchUser가 있어 클래스명까지 명시
            log.error("[fetchUser] Kakao 사용자 정보 API 통신 중 오류가 발생했습니다.", e);
            throw new GlobalException(ErrorCode.AUTH_KAKAO_USER_ERROR, e);
        }
    }

	/**
	 * 카카오 연결 해제
	 * @param kakaoSocialId SocialAuth의 socialId
	 */
	public void unlink(String kakaoSocialId) {
		// 헤더 설정
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "KakaoAK " + adminKey);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		// 파라미터 설정
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("target_id_type", "user_id");
		params.add("target_id", kakaoSocialId);

		// 요청 조립
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

		// API 호출
		try {
			ResponseEntity<String> response = restTemplate.postForEntity(UNLINK_URL, request, String.class);

			if (response.getStatusCode() == HttpStatus.OK) {
				log.info("[unlink] Kakao 연결 해제를 완료했습니다.");
				return;
			}
		} catch (HttpClientErrorException e) {
			// 카카오 측에서 보내는 4xx 에러 처리
			if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
				log.error("[unlink] Kakao 연결 해제에 실패했습니다. reason=Admin Key 인증 오류", e);
				throw new GlobalException(ErrorCode.AUTH_KAKAO_INVALID_ADMIN_KEY, e);
			} else if (e.getStatusCode() == HttpStatus.NOT_FOUND || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
				// 이미 연결이 끊겼거나 잘못된 target_id인 경우
				log.warn("[unlink] Kakao 연결이 이미 해제되었거나 대상이 올바르지 않습니다. status={}", e.getStatusCode());
				throw new GlobalException(ErrorCode.AUTH_KAKAO_USER_NOT_FOUND, e);
			}
			log.error("[unlink] Kakao 연결 해제에 실패했습니다. status={}", e.getStatusCode(), e);
			throw new GlobalException(ErrorCode.AUTH_KAKAO_UNLINK_ERROR, e);
		} catch (Exception e) {
			// 네트워크 오류나 5xx 에러 처리
			log.error("[unlink] Kakao 연결 해제 API 통신 중 오류가 발생했습니다.", e);
			throw new GlobalException(ErrorCode.AUTH_KAKAO_UNLINK_ERROR, e);
		}
	}
}
