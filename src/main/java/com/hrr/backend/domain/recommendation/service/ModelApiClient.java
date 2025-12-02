package com.hrr.backend.domain.recommendation.service;

import com.hrr.backend.domain.recommendation.dto.response.ModelApiResponse;
import com.hrr.backend.domain.recommendation.dto.request.ModelApiRequest;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;


@Service
@RequiredArgsConstructor
public class ModelApiClient {

    @Value("${model.api.url}")
    private String modelApiUrl;

    private final RestTemplate restTemplate;

    public ModelApiResponse requestRecommendations(ModelApiRequest request) {
        try {
            HttpEntity<ModelApiRequest> entity = new HttpEntity<>(request);

            ResponseEntity<ModelApiResponse> response =
                    restTemplate.exchange(modelApiUrl, HttpMethod.POST, entity, ModelApiResponse.class);

            if (response.getBody() == null) {
                throw new GlobalException(ErrorCode.EMBEDDING_API_ERROR);
            }
            return response.getBody();

            } catch (ResourceAccessException e) {
                throw new GlobalException(ErrorCode.EMBEDDING_API_ERROR);
            }
    }
}

