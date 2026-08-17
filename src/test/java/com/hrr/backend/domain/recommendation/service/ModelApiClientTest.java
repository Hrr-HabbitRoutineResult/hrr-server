package com.hrr.backend.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.hrr.backend.domain.recommendation.dto.request.ModelApiRequest;
import com.hrr.backend.domain.recommendation.dto.response.ModelApiResponse;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

class ModelApiClientTest {

    @Test
    void requestRecommendations_convertsHttpStatusFailureToEmbeddingApiError() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        ModelApiClient client = new ModelApiClient(restTemplate);
        String modelApiUrl = "http://model-server/recommend";
        ReflectionTestUtils.setField(client, "modelApiUrl", modelApiUrl);
        HttpServerErrorException failure = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(
                eq(modelApiUrl),
                eq(HttpMethod.POST),
                any(),
                eq(ModelApiResponse.class)
        )).thenThrow(failure);

        assertThatThrownBy(() -> client.requestRecommendations(mock(ModelApiRequest.class)))
                .isInstanceOfSatisfying(GlobalException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMBEDDING_API_ERROR);
                    assertThat(exception.getCause()).isSameAs(failure);
                });
    }
}
