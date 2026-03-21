package com.hrr.backend.domain.notification.listener;

import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.event.VerificationDeadlineEvent;
import com.hrr.backend.domain.notification.service.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationDeadlineNotificationListener {

    private final NotificationCommandService notificationCommandService;
    private final TaskScheduler taskScheduler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationDeadlineEvent(VerificationDeadlineEvent event) {
        LocalDateTime startAt = event.startAt();
        LocalDateTime endAt = event.endAt();
        Duration window = Duration.between(startAt, endAt);

        log.info("[인증 마감 알림] 스케줄링 시작 | RoundId={} | window={}분",
                event.roundId(), window.toMinutes());

        if (window.toHours() >= 3) {
            scheduleAt(endAt.minusHours(3), NotificationTypeName.VERIFICATION_DEADLINE_3H, event);
            scheduleAt(endAt.minusHours(1), NotificationTypeName.VERIFICATION_DEADLINE_1H, event);
        } else if (window.toHours() >= 1) {
            scheduleAt(endAt.minusHours(1), NotificationTypeName.VERIFICATION_DEADLINE_1H, event);
        } else {
            scheduleAt(startAt, NotificationTypeName.VERIFICATION_DEADLINE_NOW, event);
        }
    }

    private void scheduleAt(LocalDateTime fireAt,
                            NotificationTypeName typeName,
                            VerificationDeadlineEvent event) {

        Instant fireInstant = fireAt.atZone(ZoneId.systemDefault()).toInstant();
        LocalDate targetDate = fireAt.toLocalDate();

        if (fireInstant.isBefore(Instant.now())) {
            log.info("[인증 마감 알림] 예약 시각({})이 이미 지남 → 즉시 실행 | type={}", fireAt, typeName);
            notificationCommandService.sendDeadlineNotification(event, typeName, targetDate);
            return;
        }

        taskScheduler.schedule(
                () -> notificationCommandService.sendDeadlineNotification(event, typeName, targetDate),
                fireInstant
        );
        log.info("[인증 마감 알림] 예약 완료 | RoundId={} | type={} | fireAt={}",
                event.roundId(), typeName, fireAt);
    }
}