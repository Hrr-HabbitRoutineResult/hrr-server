package com.hrr.backend.domain.fcm.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.hrr.backend.domain.fcm.repository.FcmTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmTokenDeactivationServiceTest {

    @InjectMocks
    private FcmTokenDeactivationServiceImpl deactivationService;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Test
    @DisplayName("UNREGISTERED 에러 발생 시 해당 토큰을 비활성화해야 한다")
    void handleFailedTokens_unregistered_success() {
        // given
        String token = "unregistered-token";
        BatchResponse mockResponse = createMockBatchResponse(MessagingErrorCode.UNREGISTERED);

        // when
        deactivationService.handleFailedTokens(List.of(token), mockResponse);

        // then
        // UNREGISTERED는 토큰이 유효하지 않으므로 비활성화 호출 확인
        verify(fcmTokenRepository, times(1)).deactivateAllByToken(token);
    }

    @Test
    @DisplayName("SENDER_ID_MISMATCH 에러 발생 시 해당 토큰을 비활성화해야 한다")
    void handleFailedTokens_senderIdMismatch_success() {
        // given
        String token = "mismatch-token";
        BatchResponse mockResponse = createMockBatchResponse(MessagingErrorCode.SENDER_ID_MISMATCH);

        // when
        deactivationService.handleFailedTokens(List.of(token), mockResponse);

        // then
        // SENDER_ID_MISMATCH도 토큰 사용이 불가능하므로 비활성화 호출 확인
        verify(fcmTokenRepository, times(1)).deactivateAllByToken(token);
    }

    @Test
    @DisplayName("INVALID_ARGUMENT 에러 발생 시에는 비활성화를 수행하지 않아야 한다")
    void handleFailedTokens_invalidArgument_skip() {
        // given
        String token = "bad-payload-token";
        BatchResponse mockResponse = createMockBatchResponse(MessagingErrorCode.INVALID_ARGUMENT);

        // when
        deactivationService.handleFailedTokens(List.of(token), mockResponse);

        // then
        // INVALID_ARGUMENT는 토큰을 유지
        verify(fcmTokenRepository, never()).deactivateAllByToken(anyString());
    }

    @Test
    @DisplayName("INTERNAL 에러 등 기타 일시적 오류 발생 시에는 비활성화를 수행하지 않는다")
    void handleFailedTokens_internalError_skip() {
        // given
        String token = "temporary-error-token";
        BatchResponse mockResponse = createMockBatchResponse(MessagingErrorCode.INTERNAL);

        // when
        deactivationService.handleFailedTokens(List.of(token), mockResponse);

        // then
        // 토큰 자체의 문제가 아니므로 비활성화하지 않음
        verify(fcmTokenRepository, never()).deactivateAllByToken(anyString());
    }

    /**
     * 특정 에러 코드를 포함한 BatchResponse를 생성하는 헬퍼 메서드
     */
    private BatchResponse createMockBatchResponse(MessagingErrorCode errorCode) {
        // 에러 코드를 가진 Exception 모킹
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        lenient().when(exception.getMessagingErrorCode()).thenReturn(errorCode);

        // 실패 응답 모킹
        SendResponse failResponse = mock(SendResponse.class);
        lenient().when(failResponse.isSuccessful()).thenReturn(false);
        lenient().when(failResponse.getException()).thenReturn(exception);

        // BatchResponse가 실패 응답 리스트를 반환하도록 설정
        BatchResponse batchResponse = mock(BatchResponse.class);
        lenient().when(batchResponse.getResponses()).thenReturn(List.of(failResponse));

        return batchResponse;
    }
}