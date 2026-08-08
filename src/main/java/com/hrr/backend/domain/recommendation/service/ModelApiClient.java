package com.hrr.backend.domain.recommendation.service;

import com.hrr.backend.domain.recommendation.dto.response.ModelApiResponse;
import com.hrr.backend.domain.recommendation.dto.request.ModelApiRequest;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Service
public class ModelApiClient {

    @Value("${model.api.url}")
    private String modelApiUrl;

    private final RestTemplate restTemplate;

    public ModelApiClient(@Qualifier("modelApiRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ModelApiResponse requestRecommendations(ModelApiRequest request) {
        try {
            HttpEntity<ModelApiRequest> entity = new HttpEntity<>(request);

            ResponseEntity<ModelApiResponse> response =
                    restTemplate.exchange(modelApiUrl, HttpMethod.POST, entity, ModelApiResponse.class);

            if (response.getBody() == null) {
                log.error("[requestRecommendations] 추천 Model API 응답 body가 비어 있습니다.");
                throw new GlobalException(ErrorCode.EMBEDDING_API_ERROR);
            }
            return response.getBody();

            } catch (ResourceAccessException e) {
                log.error("[requestRecommendations] 추천 Model API 통신 중 오류가 발생했습니다.", e);
                throw new GlobalException(ErrorCode.EMBEDDING_API_ERROR, e);
            }
    }
}
