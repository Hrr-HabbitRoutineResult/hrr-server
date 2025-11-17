package com.hrr.backend.domain.recommendation.service;

import com.hrr.backend.domain.recommendation.dto.response.ModelApiResponse;
import com.hrr.backend.domain.recommendation.dto.request.ModelApiRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class ModelApiClient {

    @Value("${model.api.url}")
    private String modelApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public ModelApiResponse requestRecommendations(ModelApiRequest request) {
        HttpEntity<ModelApiRequest> entity = new HttpEntity<>(request);

        ResponseEntity<ModelApiResponse> response =
                restTemplate.exchange(modelApiUrl, HttpMethod.POST, entity, ModelApiResponse.class);

        return response.getBody();
    }
}

