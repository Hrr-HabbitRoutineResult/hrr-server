package com.hrr.backend.domain.notification.service;

import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.event.CommentCreatedEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionResponseEvent;
import com.hrr.backend.domain.notification.event.QuestionVerificationCreatedEvent;
import com.hrr.backend.domain.notification.event.VerificationDeadlineEvent;
import java.time.LocalDate;

public interface NotificationCommandService {
    // 인증 마감 알림 발송
    void sendDeadlineNotification(VerificationDeadlineEvent event, NotificationTypeName typeName, LocalDate targetDate);

    // 챌린지 연장 안내 발송
    void sendChallengeExtensionNotification(ChallengeExtensionEvent event);

    // 챌린지 연장 응답 결과 발송
    void sendChallengeExtensionResponseNotification(ChallengeExtensionResponseEvent event);

    // 인증 댓글 생성 알림 발송
    void sendCommentCreatedNotification(CommentCreatedEvent event);

    // 질문 인증 생성 알림 발송
    void sendQuestionVerificationCreatedNotification(QuestionVerificationCreatedEvent event);
}
