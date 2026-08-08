package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.dto.EmbeddingRequestDto;
import com.hrr.backend.domain.challenge.dto.EmbeddingResponseDto;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.exception.GlobalException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class EmbeddingClient {

    @Value("${model.api.embedding-url}")
    private String embeddingUrl;

    private final RestTemplate restTemplate;

    public EmbeddingClient(@Qualifier("modelApiRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public float[] getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new GlobalException(ErrorCode.EMBEDDING_INVALID_INPUT);
        }

        try {
            long startedAt = System.nanoTime();
            EmbeddingRequestDto request = new EmbeddingRequestDto(text);
            EmbeddingResponseDto response =
                    restTemplate.postForObject(embeddingUrl, request, EmbeddingResponseDto.class);

            if (response == null || response.getEmbedding() == null) {
                throw new GlobalException(ErrorCode.EMBEDDING_API_ERROR);
            }

            float[] embedding = response.getEmbedding();
            log.info("[getEmbedding] 임베딩 Model API 응답을 받았습니다. dimension={}, latencyMs={}",
                    embedding.length, (System.nanoTime() - startedAt) / 1_000_000);
            return embedding;
        } catch (RestClientException e) {
            // 상위 @Retryable 흐름에서 각 시도는 WARN, 최종 실패만 ERROR로 집계한다.
            throw new GlobalException(ErrorCode.EMBEDDING_API_ERROR, e);
        }
    }

}
