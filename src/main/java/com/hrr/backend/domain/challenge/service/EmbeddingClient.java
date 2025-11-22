package com.hrr.backend.domain.challenge.service;


import com.hrr.backend.domain.challenge.dto.EmbeddingRequestDto;
import com.hrr.backend.domain.challenge.dto.EmbeddingResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    @Value("${model.api.embedding-url}")
    private String embeddingUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public float[] getEmbedding(String text) {
        EmbeddingRequestDto request = new EmbeddingRequestDto(text);
        EmbeddingResponseDto response =
                restTemplate.postForObject(embeddingUrl, request, EmbeddingResponseDto.class);

        return response.getEmbedding();
    }
}