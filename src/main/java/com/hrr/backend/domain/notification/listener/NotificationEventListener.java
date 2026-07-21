package com.hrr.backend.domain.notification.listener;

import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionResponseEvent;
import com.hrr.backend.domain.notification.event.ChallengeStartEvent;
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
     * 챌린지 시작 하루 전 안내 이벤트 핸들러
     * 트랜잭션 커밋 후 비동기로 실행되어 실제 알림 발송 로직을 호출
     */
    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChallengeStartEvent(ChallengeStartEvent event) {
        log.info("[알림 이벤트 리스너] 챌린지 시작 하루 전 알림 처리 시작 | ChallengeId={}", event.challengeId());
        notificationCommandService.sendChallengeStartNotification(event);
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
}
