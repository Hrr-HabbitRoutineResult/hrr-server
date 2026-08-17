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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
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
                throw new GlobalException(ErrorCode.EMBEDDING_API_SERVER_ERROR);
            }
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.warn("[requestRecommendations] 추천 Model API가 요청을 거부했습니다. status={}, exception={}",
                    e.getStatusCode(), e.getClass().getSimpleName());
            throw new GlobalException(ErrorCode.EMBEDDING_API_ERROR, e);
        } catch (HttpServerErrorException e) {
            log.error("[requestRecommendations] 추천 Model API 서버에서 오류가 발생했습니다. status={}, exception={}",
                    e.getStatusCode(), e.getClass().getSimpleName());
            throw new GlobalException(ErrorCode.EMBEDDING_API_SERVER_ERROR, e);
        } catch (ResourceAccessException e) {
            log.error("[requestRecommendations] 추천 Model API에 연결할 수 없습니다. exception={}",
                    e.getClass().getSimpleName());
            throw new GlobalException(ErrorCode.EMBEDDING_API_UNAVAILABLE, e);
        } catch (RestClientException e) {
            log.error("[requestRecommendations] 추천 Model API 응답 처리 중 오류가 발생했습니다. exception={}",
                    e.getClass().getSimpleName());
            throw new GlobalException(ErrorCode.EMBEDDING_API_SERVER_ERROR, e);
        }
    }
}
