package com.hrr.backend.domain.fcm.service;

import com.google.firebase.messaging.BatchResponse;
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

        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);

            if (!sendResponse.isSuccessful()) {
                MessagingErrorCode errorCode = sendResponse.getException().getMessagingErrorCode();
                String token = tokens.get(i);

                if (errorCode == MessagingErrorCode.UNREGISTERED ||
                        errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {

                    // 토큰 자체가 무효한 경우 비활성화 수행
                    fcmTokenRepository.deactivateAllByToken(token);
                    log.info("유효하지 않은 토큰 비활성화 처리됨 ({}) : token={}", errorCode, maskToken(token));

                } else if (errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                    // 페이로드 오류 등은 로그만 남기고 토큰은 유지
                    log.warn("FCM 페이로드 오류 (INVALID_ARGUMENT): token={}, message={}",
                            maskToken(token), sendResponse.getException().getMessage());

                } else {
                    // 기타 일시적 오류 등
                    log.warn("FCM 발송 실패 (유지 대상): errorCode={}, message={}",
                            errorCode, sendResponse.getException().getMessage());
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