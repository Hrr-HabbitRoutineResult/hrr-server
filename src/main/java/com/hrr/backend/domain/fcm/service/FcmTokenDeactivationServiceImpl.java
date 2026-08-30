package com.hrr.backend.domain.fcm.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import com.hrr.backend.domain.fcm.dto.FcmTokenTargetDto;
import com.hrr.backend.domain.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenDeactivationServiceImpl implements FcmTokenDeactivationService {

    private final FcmTokenRepository fcmTokenRepository;

    @Override
    // 실패 토큰 갱신이 알림 생성 트랜잭션과 서로 영향을 주지 않도록 분리한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailedTokens(List<FcmTokenTargetDto> targets, BatchResponse response) {
        String incidentId = UUID.randomUUID().toString();
        List<SendResponse> responses = response != null ? response.getResponses() : null;

        // 응답 인덱스로 내부 대상을 찾으므로 두 목록의 크기가 반드시 같아야 한다.
        if (targets == null || responses == null || targets.size() != responses.size()) {
            log.error("[handleFailedTokens] FCM 대상 목록과 응답 목록의 크기가 일치하지 않습니다. incidentId={}, targetCount={}, responseCount={}",
                    incidentId,
                    targets != null ? targets.size() : "null",
                    responses != null ? responses.size() : "null");
            return;
        }

        int actionableFailureCount = 0;
        Map<String, Integer> failureBreakdown = new LinkedHashMap<>();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);

            if (!sendResponse.isSuccessful()) {
                FcmTokenTargetDto target = targets.get(i);

                // NPE 방지
                FirebaseMessagingException exception = sendResponse.getException();
                if (exception == null) {
                    String failureKey = "messaging=UNKNOWN,platform=UNKNOWN,http=UNKNOWN";
                    log.warn("[handleFailedTokens] FCM 실패 응답에 예외 정보가 없습니다. incidentId={}, responseIndex={}, fcmTokenId={}, userId={}, action=RETAIN",
                            incidentId, i, target.fcmTokenId(), target.userId());
                    actionableFailureCount++;
                    failureBreakdown.merge(failureKey, 1, Integer::sum);
                    continue; // 원인 파악이 불가능하므로 건너뜀
                }

                MessagingErrorCode errorCode = exception.getMessagingErrorCode();
                String messagingCode = errorCode != null ? errorCode.name() : "UNKNOWN";
                String platformCode = exception.getErrorCode() != null ? exception.getErrorCode().name() : "UNKNOWN";
                String httpStatus = exception.getHttpResponse() != null
                        ? Integer.toString(exception.getHttpResponse().getStatusCode())
                        : "UNKNOWN";
                String failureKey = "messaging=" + messagingCode + ",platform=" + platformCode + ",http=" + httpStatus;

                if (errorCode == MessagingErrorCode.UNREGISTERED ||
                        errorCode == MessagingErrorCode.SENDER_ID_MISMATCH) {

                    // 토큰 자체가 무효한 경우 비활성화 수행
                    fcmTokenRepository.deactivateAllByToken(target.token());
                    log.warn("[handleFailedTokens] 유효하지 않은 FCM token을 비활성화했습니다. incidentId={}, messagingCode={}, platformCode={}, httpStatus={}, responseIndex={}, fcmTokenId={}, userId={}",
                            incidentId, messagingCode, platformCode, httpStatus, i,
                            target.fcmTokenId(), target.userId());
                } else {
                    // 페이로드 오류, 일시적 오류 및 분류되지 않은 오류는 진단 정보를 남기고 토큰을 유지한다.
                    // 내부 ID는 서버 WARN에만 남기고 Discord로 전송되는 집계 ERROR에서는 제외한다.
                    log.warn("[handleFailedTokens] FCM 발송에 실패했지만 token을 유지합니다. incidentId={}, messagingCode={}, platformCode={}, httpStatus={}, responseIndex={}, fcmTokenId={}, userId={}, exceptionType={}, action=RETAIN",
                            incidentId, messagingCode, platformCode, httpStatus, i,
                            target.fcmTokenId(), target.userId(), exception.getClass().getSimpleName());
                    actionableFailureCount++;
                    failureBreakdown.merge(failureKey, 1, Integer::sum);
                }
            }
        }
        if (actionableFailureCount > 0) {
            log.error("[handleFailedTokens] FCM 응답에서 조치가 필요한 실패가 누적되었습니다. incidentId={}, failureCount={}, responseCount={}, breakdown={}",
                    incidentId, actionableFailureCount, responses.size(), failureBreakdown);
        }
    }

}
