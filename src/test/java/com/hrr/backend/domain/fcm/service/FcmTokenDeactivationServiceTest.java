package com.hrr.backend.domain.fcm.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.google.firebase.ErrorCode;
import com.google.firebase.IncomingHttpResponse;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.hrr.backend.domain.fcm.dto.FcmTokenTargetDto;
import com.hrr.backend.domain.fcm.repository.FcmTokenRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

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
        FcmTokenTargetDto target = target(11L, 101L, token);
        BatchResponse mockResponse = createMockBatchResponse(MessagingErrorCode.UNREGISTERED);

        // when
        deactivationService.handleFailedTokens(List.of(target), mockResponse);

        // then
        // UNREGISTERED는 토큰이 유효하지 않으므로 비활성화 호출 확인
        verify(fcmTokenRepository, times(1)).deactivateAllByToken(token);
    }

    @Test
    @DisplayName("SENDER_ID_MISMATCH 에러 발생 시 해당 토큰을 비활성화해야 한다")
    void handleFailedTokens_senderIdMismatch_success() {
        // given
        String token = "mismatch-token";
        FcmTokenTargetDto target = target(12L, 102L, token);
        BatchResponse mockResponse = createMockBatchResponse(MessagingErrorCode.SENDER_ID_MISMATCH);

        // when
        deactivationService.handleFailedTokens(List.of(target), mockResponse);

        // then
        // SENDER_ID_MISMATCH도 토큰 사용이 불가능하므로 비활성화 호출 확인
        verify(fcmTokenRepository, times(1)).deactivateAllByToken(token);
    }

    @Test
    @DisplayName("INVALID_ARGUMENT 에러 발생 시에는 비활성화를 수행하지 않아야 한다")
    void handleFailedTokens_invalidArgument_skip() {
        // given
        String token = "bad-payload-token";
        FcmTokenTargetDto target = target(13L, 103L, token);
        BatchResponse mockResponse = createMockBatchResponse(MessagingErrorCode.INVALID_ARGUMENT);

        // when
        deactivationService.handleFailedTokens(List.of(target), mockResponse);

        // then
        // INVALID_ARGUMENT는 토큰을 유지
        verify(fcmTokenRepository, never()).deactivateAllByToken(anyString());
    }

    @Test
    @DisplayName("INTERNAL 에러 등 기타 일시적 오류 발생 시에는 비활성화를 수행하지 않는다")
    void handleFailedTokens_internalError_skip() {
        // given
        String token = "temporary-error-token";
        FcmTokenTargetDto target = target(14L, 104L, token);
        BatchResponse mockResponse = createMockBatchResponse(MessagingErrorCode.INTERNAL);

        // when
        deactivationService.handleFailedTokens(List.of(target), mockResponse);

        // then
        // 토큰 자체의 문제가 아니므로 비활성화하지 않음
        verify(fcmTokenRepository, never()).deactivateAllByToken(anyString());
    }

    @Test
    @DisplayName("메시징 코드가 없어도 안전한 진단 정보는 WARN에 남기고 집계 ERROR에는 원문 토큰과 예외를 남기지 않는다")
    void handleFailedTokens_nullMessagingCode_logsSafeDetails() {
        String token = "sensitive-fcm-token-value";
        FcmTokenTargetDto target = target(15L, 105L, token);
        Assertions.assertThat(target.toString()).contains("token=[REDACTED]").doesNotContain(token);

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(null);
        when(exception.getErrorCode()).thenReturn(ErrorCode.NOT_FOUND);
        IncomingHttpResponse httpResponse = mock(IncomingHttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(404);
        when(exception.getHttpResponse()).thenReturn(httpResponse);

        SendResponse failedResponse = mock(SendResponse.class);
        when(failedResponse.isSuccessful()).thenReturn(false);
        when(failedResponse.getException()).thenReturn(exception);

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getResponses()).thenReturn(List.of(failedResponse));

        Logger logger = (Logger) LoggerFactory.getLogger(FcmTokenDeactivationServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            deactivationService.handleFailedTokens(List.of(target), batchResponse);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        ILoggingEvent warning = appender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .findFirst()
                .orElseThrow();
        ILoggingEvent aggregateError = appender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR)
                .findFirst()
                .orElseThrow();

        String warningMessage = warning.getFormattedMessage();
        String aggregateMessage = aggregateError.getFormattedMessage();
        String incidentId = extractField(warningMessage, "incidentId");

        Assertions.assertThat(warningMessage)
                .contains("messagingCode=UNKNOWN", "platformCode=NOT_FOUND", "httpStatus=404",
                        "fcmTokenId=15", "userId=105")
                .doesNotContain(token);
        Assertions.assertThat(aggregateMessage)
                .contains("incidentId=" + incidentId, "failureCount=1", "responseCount=1",
                        "platform=NOT_FOUND", "http=404")
                .doesNotContain(token, "fcmTokenId=", "userId=");
        Assertions.assertThat(aggregateError.getThrowableProxy()).isNull();
    }

    private FcmTokenTargetDto target(Long fcmTokenId, Long userId, String token) {
        return new FcmTokenTargetDto(fcmTokenId, userId, token);
    }

    private String extractField(String message, String fieldName) {
        String prefix = fieldName + "=";
        int start = message.indexOf(prefix);
        int end = message.indexOf(',', start);
        return end >= 0
                ? message.substring(start + prefix.length(), end)
                : message.substring(start + prefix.length());
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
