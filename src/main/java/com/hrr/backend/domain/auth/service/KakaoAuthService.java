package com.hrr.backend.domain.auth.service;

import com.hrr.backend.domain.auth.dto.KakaoTokenResponse;
import com.hrr.backend.domain.auth.dto.KakaoUserResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoAuthService {
    //private final WebClient webClient = WebClient.create();
    private  final WebClient webClient;
    public KakaoAuthService() {
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

    @Value("${kakao.client-id}")  private String clientId;       // REST API 키
    @Value("${kakao.redirect-uri}") private String redirectUri;  // hrr://oauth/kakao
    @Value("${KAKAO_CLIENT_SECRET}") private String clientSecret;
    public KakaoTokenResponse exchangeToken(String code) {
            return webClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                            .with("client_id", clientId)
                            .with("redirect_uri", redirectUri)
                            .with("code", code)
                            .with("client_secret", clientSecret))
                    .retrieve() // webClient.post() 바로 뒤에 와야 함
                    .bodyToMono(KakaoTokenResponse.class)
                    .block();
    }

    public KakaoUserResponse fetchUser(String accessToken) {
        return webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponse.class)
                .block();
    }
}