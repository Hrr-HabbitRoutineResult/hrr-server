package com.hrr.backend.domain.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.hrr.backend.domain.recommendation.dto.request.ModelApiRequest;
import com.hrr.backend.domain.recommendation.dto.response.ModelApiResponse;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

class ModelApiClientTest {

    private static final String MODEL_API_URL = "http://model-server/recommend";

    private RestTemplate restTemplate;
    private ModelApiClient client;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        client = new ModelApiClient(restTemplate);
        ReflectionTestUtils.setField(client, "modelApiUrl", MODEL_API_URL);
    }

    @Test
    void requestRecommendations_convertsClientErrorToBadRequest() {
        HttpClientErrorException failure = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        mockFailure(failure);

        assertFailure(failure, ErrorCode.EMBEDDING_API_ERROR, HttpStatus.BAD_REQUEST);
    }

    @Test
    void requestRecommendations_convertsServerErrorToBadGateway() {
        HttpServerErrorException failure = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        mockFailure(failure);

        assertFailure(failure, ErrorCode.EMBEDDING_API_SERVER_ERROR, HttpStatus.BAD_GATEWAY);
    }

    @Test
    void requestRecommendations_convertsNetworkFailureToServiceUnavailable() {
        ResourceAccessException failure = new ResourceAccessException("connection failed");
        mockFailure(failure);

        assertFailure(failure, ErrorCode.EMBEDDING_API_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void requestRecommendations_convertsEmptyBodyToBadGateway() {
        when(restTemplate.exchange(
                eq(MODEL_API_URL),
                eq(HttpMethod.POST),
                any(),
                eq(ModelApiResponse.class)
        )).thenReturn(ResponseEntity.<ModelApiResponse>ok().build());

        assertThatThrownBy(() -> client.requestRecommendations(mock(ModelApiRequest.class)))
                .isInstanceOfSatisfying(GlobalException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMBEDDING_API_SERVER_ERROR);
                    assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.BAD_GATEWAY);
                });
    }

    private void mockFailure(RestClientException failure) {
        when(restTemplate.exchange(
                eq(MODEL_API_URL),
                eq(HttpMethod.POST),
                any(),
                eq(ModelApiResponse.class)
        )).thenThrow(failure);
    }

    private void assertFailure(RestClientException failure, ErrorCode errorCode, HttpStatus httpStatus) {
        assertThatThrownBy(() -> client.requestRecommendations(mock(ModelApiRequest.class)))
                .isInstanceOfSatisfying(GlobalException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(errorCode);
                    assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(httpStatus);
                    assertThat(exception.getCause()).isSameAs(failure);
                });
    }
}
