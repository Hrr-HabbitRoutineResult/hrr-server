package com.hrr.backend.domain.fcm.service;

import com.google.firebase.messaging.BatchResponse;
import com.hrr.backend.domain.fcm.dto.FcmTokenTargetDto;
import java.util.List;

public interface FcmTokenDeactivationService {
    /**
     * FCM 발송 결과에서 실패한 토큰들이 유효하지 않은 경우 비활성화 처리
     */
    void handleFailedTokens(List<FcmTokenTargetDto> targets, BatchResponse response);
}
