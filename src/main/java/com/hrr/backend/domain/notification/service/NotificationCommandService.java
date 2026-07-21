package com.hrr.backend.domain.notification.service;

import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeStartEvent;
import com.hrr.backend.domain.notification.event.ChallengeUpdatedEvent;
import com.hrr.backend.domain.notification.event.VerificationDeadlineEvent;
import java.time.LocalDate;

public interface NotificationCommandService {
    // 인증 마감 알림 발송
    void sendDeadlineNotification(VerificationDeadlineEvent event, NotificationTypeName typeName, LocalDate targetDate);

    // 챌린지 연장 안내 발송
    void sendChallengeExtensionNotification(ChallengeExtensionEvent event);

    // 챌린지 시작 하루 전 안내 발송
    void sendChallengeStartNotification(ChallengeStartEvent event);

    // 챌린지 수정 안내 발송
    void sendChallengeUpdatedNotification(ChallengeUpdatedEvent event);

}
