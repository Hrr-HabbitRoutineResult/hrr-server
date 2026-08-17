package com.hrr.backend.domain.notification.listener;

import com.hrr.backend.domain.notification.event.CommentCreatedEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeStartEvent;
import com.hrr.backend.domain.notification.event.ChallengeUpdatedEvent;
import com.hrr.backend.domain.notification.event.ChallengeVacancyEvent;
import com.hrr.backend.domain.notification.event.QuestionVerificationCreatedEvent;
import com.hrr.backend.domain.notification.event.WeakVerificationWarningEvent;
import com.hrr.backend.domain.notification.event.FollowCreatedEvent;
import com.hrr.backend.domain.notification.service.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationCommandService notificationCommandService;

    /**
     * 챌린지 연장 안내 이벤트 핸들러
     * 트랜잭션 커밋 후 비동기로 실행되어 실제 알림 발송 로직을 호출
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChallengeExtensionEvent(ChallengeExtensionEvent event) {
        notificationCommandService.sendChallengeExtensionNotification(event);
    }

    /**
     * 챌린지 시작 하루 전 안내 이벤트 핸들러
     * 트랜잭션 커밋 후 비동기로 실행되어 실제 알림 발송 로직을 호출
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChallengeStartEvent(ChallengeStartEvent event) {
        notificationCommandService.sendChallengeStartNotification(event);
    }

    /**
     * 챌린지 수정 안내 이벤트 핸들러
     * 트랜잭션 커밋 후 비동기로 실행되어 실제 알림 발송 로직을 호출
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChallengeUpdatedEvent(ChallengeUpdatedEvent event) {
        notificationCommandService.sendChallengeUpdatedNotification(event);
    }

    /**
     * 챌린지 빈자리 발생 이벤트 핸들러
     * 정원 상태에서 참여자 이탈 후 비동기로 빈자리 알림을 생성
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChallengeVacancyEvent(ChallengeVacancyEvent event) {
        notificationCommandService.sendChallengeVacancyNotification(event);
    }

    /**
     * 인증 댓글 생성 이벤트 핸들러
     * 댓글 생성 트랜잭션 커밋 후 인증 작성자에게 알림을 발송
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        notificationCommandService.sendCommentCreatedNotification(event);
    }

    /**
     * 질문 인증 생성 이벤트 핸들러
     * 질문 인증 생성 트랜잭션 커밋 후 같은 챌린지 참여자에게 알림을 발송
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuestionVerificationCreatedEvent(QuestionVerificationCreatedEvent event) {
        notificationCommandService.sendQuestionVerificationCreatedNotification(event);
    }

    /**
     * 부실 인증 경고 이벤트 핸들러
     * 부실 인증 신고 누적으로 경고가 증가한 사용자에게 알림을 발송
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWeakVerificationWarningEvent(WeakVerificationWarningEvent event) {
        notificationCommandService.sendWeakVerificationWarningNotification(event);
    }

    /**
     * 팔로우 생성 이벤트 핸들러
     * 팔로우 성립 후 비동기로 새 팔로워 알림을 생성
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFollowCreatedEvent(FollowCreatedEvent event) {
        notificationCommandService.sendFollowCreatedNotification(event);
    }

}
