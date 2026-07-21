package com.hrr.backend.domain.notification.service;

import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.event.CommentCreatedEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.QuestionVerificationCreatedEvent;
import com.hrr.backend.domain.notification.event.FollowCreatedEvent;
import com.hrr.backend.domain.notification.event.ChallengeStartEvent;
import com.hrr.backend.domain.notification.event.ChallengeUpdatedEvent;
import com.hrr.backend.domain.notification.event.VerificationDeadlineEvent;
import com.hrr.backend.domain.notification.event.WeakVerificationWarningEvent;
import java.time.LocalDate;

public interface NotificationCommandService {
    // 인증 마감 알림 발송
    void sendDeadlineNotification(VerificationDeadlineEvent event, NotificationTypeName typeName, LocalDate targetDate);

    // 챌린지 연장 안내 발송
    void sendChallengeExtensionNotification(ChallengeExtensionEvent event);

    // 인증 댓글 생성 알림 발송
    void sendCommentCreatedNotification(CommentCreatedEvent event);

    // 질문 인증 생성 알림 발송
    void sendQuestionVerificationCreatedNotification(QuestionVerificationCreatedEvent event);

    // 부실 인증 경고 알림 발송
    void sendWeakVerificationWarningNotification(WeakVerificationWarningEvent event);

    // 팔로우 생성 알림 발송
    void sendFollowCreatedNotification(FollowCreatedEvent event);

    // 챌린지 시작 하루 전 안내 발송
    void sendChallengeStartNotification(ChallengeStartEvent event);

    // 챌린지 수정 안내 발송
    void sendChallengeUpdatedNotification(ChallengeUpdatedEvent event);

}
