package com.hrr.backend.domain.notification.listener;

import com.hrr.backend.domain.notification.event.CommentCreatedEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionResponseEvent;
import com.hrr.backend.domain.notification.event.QuestionVerificationCreatedEvent;
import com.hrr.backend.domain.notification.event.WeakVerificationWarningEvent;
import com.hrr.backend.domain.notification.service.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
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
        log.info("[알림 이벤트 리스너] 챌린지 연장 안내 알림 처리 시작 | RoundId={}", event.roundId());
        notificationCommandService.sendChallengeExtensionNotification(event);
    }

    /**
     * 챌린지 연장 응답 결과 이벤트 핸들러
     * 사용자의 선택(연장/취소)에 따른 결과를 비동기로 발송
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChallengeExtensionResponseEvent(ChallengeExtensionResponseEvent event) {
        log.info("[알림 이벤트 리스너] 챌린지 연장 응답 결과 알림 처리 시작 | RoundId={}, UserId={}",
                event.roundId(), event.user().getId());
        notificationCommandService.sendChallengeExtensionResponseNotification(event);
    }

    /**
     * 인증 댓글 생성 이벤트 핸들러
     * 댓글 생성 트랜잭션 커밋 후 인증 작성자에게 알림을 발송
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreatedEvent(CommentCreatedEvent event) {
        log.info("[알림 이벤트 리스너] 인증 댓글 생성 알림 처리 시작 | VerificationId={}, CommentId={}",
                event.verificationId(), event.commentId());
        notificationCommandService.sendCommentCreatedNotification(event);
    }

    /**
     * 질문 인증 생성 이벤트 핸들러
     * 질문 인증 생성 트랜잭션 커밋 후 같은 챌린지 참여자에게 알림을 발송
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuestionVerificationCreatedEvent(QuestionVerificationCreatedEvent event) {
        log.info("[알림 이벤트 리스너] 질문 인증 생성 알림 처리 시작 | VerificationId={}",
                event.verificationId());
        notificationCommandService.sendQuestionVerificationCreatedNotification(event);
    }

    /**
     * 부실 인증 경고 이벤트 핸들러
     * 부실 인증 신고 누적으로 경고가 증가한 사용자에게 알림을 발송
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleWeakVerificationWarningEvent(WeakVerificationWarningEvent event) {
        log.info("[알림 이벤트 리스너] 부실 인증 경고 알림 처리 시작 | VerificationId={}, WarnedUserId={}",
                event.verificationId(), event.warnedUserId());
        notificationCommandService.sendWeakVerificationWarningNotification(event);
    }
}
