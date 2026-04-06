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
            log.error("FCM 토큰 리스트와 응답 리스트의 크기가 일치하지 않거나 데이터가 누락되었습니다. (tokens={}, responses={})",
                    tokens != null ? tokens.size() : "null",
                    responses != null ? responses.size() : "null");
            return;
        }

        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);

            if (!sendResponse.isSuccessful()) {
                String token = tokens.get(i);

                // NPE 방지
                FirebaseMessagingException exception = sendResponse.getException();
                if (exception == null) {
                    log.warn("FCM 발송 실패했으나 예외 정보가 누락되었습니다: token={}", maskToken(token));
                    continue; // 원인 파악이 불가능하므로 건너뜀
                }

                MessagingErrorCode errorCode = exception.getMessagingErrorCode();

                if (errorCode == MessagingErrorCode.UNREGISTERED ||
                        errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {

                    // 토큰 자체가 무효한 경우 비활성화 수행
                    fcmTokenRepository.deactivateAllByToken(token);
                    log.info("유효하지 않은 토큰 비활성화 처리됨 ({}) : token={}", errorCode, maskToken(token));

                } else if (errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    // 페이로드 오류 등은 로그만 남기고 토큰은 유지
                    log.warn("FCM 페이로드 오류 (INVALID_ARGUMENT): token={}, message={}",
                            maskToken(token), exception.getMessage());
                } else {
                    // 기타 일시적 오류 등
                    log.warn("FCM 발송 실패 (유지 대상): errorCode={}, message={}",
                            errorCode, exception.getMessage());
                }
            }
        }
    }

    private String maskToken(String token) {
        if (token == null || token.isBlank()) {
            return "<empty>";
        }
        int visible = Math.min(20, token.length());
        return token.substring(0, visible) + "...";
    }
}