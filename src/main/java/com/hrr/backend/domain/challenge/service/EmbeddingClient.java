package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.dto.EmbeddingRequestDto;
import com.hrr.backend.domain.challenge.dto.EmbeddingResponseDto;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.exception.GlobalException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;


@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    @Value("${model.api.embedding-url}")
    private String embeddingUrl;

    @Qualifier("modelApiRestTemplate")
    private final RestTemplate restTemplate;

    public float[] getEmbedding(String text) {
        if (text == null || text.isBlank()) {
            throw new GlobalException(ErrorCode.EMBEDDING_INVALID_INPUT);
        }

        try {
            EmbeddingRequestDto request = new EmbeddingRequestDto(text);
            EmbeddingResponseDto response =
                    restTemplate.postForObject(embeddingUrl, request, EmbeddingResponseDto.class);

            if (response == null || response.getEmbedding() == null) {
                throw new GlobalException(ErrorCode.EMBEDDING_API_ERROR);
            }

            return response.getEmbedding();
        } catch (RestClientException e) {
            throw new GlobalException(ErrorCode.EMBEDDING_API_ERROR);
        }
    }

}