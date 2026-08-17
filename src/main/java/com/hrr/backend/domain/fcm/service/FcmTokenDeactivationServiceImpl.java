package com.hrr.backend.domain.fcm.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.hrr.backend.domain.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenDeactivationServiceImpl implements FcmTokenDeactivationService {

    private final FcmTokenRepository fcmTokenRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 별도의 독립적인 트랜잭션 보장
    public void handleFailedTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();

        // 토큰 리스트와 응답 리스트의 크기 정합성 체크
        if (tokens == null || responses == null || tokens.size() != responses.size()) {
            log.error("[handleFailedTokens] FCM token 목록과 응답 목록의 크기가 일치하지 않습니다. tokenCount={}, responseCount={}",
                    tokens != null ? tokens.size() : "null",
                    responses != null ? responses.size() : "null");
            return;
        }

        int actionableFailureCount = 0;
        FirebaseMessagingException firstFailure = null;
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);

            if (!sendResponse.isSuccessful()) {
                String token = tokens.get(i);

                // NPE 방지
                FirebaseMessagingException exception = sendResponse.getException();
                if (exception == null) {
                    log.warn("[handleFailedTokens] FCM 실패 응답에 예외 정보가 없습니다. responseIndex={}", i);
                    actionableFailureCount++;
                    continue; // 원인 파악이 불가능하므로 건너뜀
                }

                MessagingErrorCode errorCode = exception.getMessagingErrorCode();

                if (errorCode == MessagingErrorCode.UNREGISTERED ||
                        errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {

                    // 토큰 자체가 무효한 경우 비활성화 수행
                    fcmTokenRepository.deactivateAllByToken(token);
                    log.info("[handleFailedTokens] 유효하지 않은 FCM token을 비활성화했습니다. errorCode={}, responseIndex={}",
                            errorCode, i);
                } else if (errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    // 페이로드 오류 등은 로그만 남기고 토큰은 유지
                    log.warn("[handleFailedTokens] FCM payload가 유효하지 않습니다. errorCode={}, responseIndex={}",
                            errorCode, i);
                    actionableFailureCount++;
                    if (firstFailure == null) firstFailure = exception;
                } else {
                    // 기타 일시적 오류 등
                    log.warn("[handleFailedTokens] FCM 발송에 실패했지만 token을 유지합니다. errorCode={}, exception={}",
                            errorCode, exception.getClass().getSimpleName());
                    actionableFailureCount++;
                    if (firstFailure == null) firstFailure = exception;
                }
            }
        }
        if (actionableFailureCount > 0) {
            if (firstFailure != null) {
                log.error("[handleFailedTokens] FCM 응답에서 조치가 필요한 실패가 누적되었습니다. failureCount={}, responseCount={}",
                        actionableFailureCount, responses.size(), firstFailure);
            } else {
                log.error("[handleFailedTokens] FCM 응답에서 원인을 확인할 수 없는 실패가 누적되었습니다. failureCount={}, responseCount={}",
                        actionableFailureCount, responses.size());
            }
        }
    }

}
