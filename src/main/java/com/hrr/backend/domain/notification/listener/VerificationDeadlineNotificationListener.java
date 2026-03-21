package com.hrr.backend.domain.notification.listener;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.fcm.event.FcmPushSendEvent;
import com.hrr.backend.domain.notification.entity.*;
import com.hrr.backend.domain.notification.entity.enums.*;
import com.hrr.backend.domain.notification.event.VerificationDeadlineEvent;
import com.hrr.backend.domain.notification.repository.*;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationDeadlineNotificationListener {

    private final RoundRepository roundRepository;
    private final RoundRecordRepository roundRecordRepository;
    private final NotificationTypeRepository typeRepository;
    private final NotificationEventRepository notificationEventRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskScheduler taskScheduler;

    // 인증 시간 길이에 따라 알림 시각 결정 후 스케줄 등록
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationDeadlineEvent(VerificationDeadlineEvent event) {
        LocalDateTime startAt = event.startAt();
        LocalDateTime endAt   = event.endAt();
        Duration window       = Duration.between(startAt, endAt);

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

    // 스케줄 등록 (이미 지난 시각이면 즉시 실행)
    private void scheduleAt(LocalDateTime fireAt,
                            NotificationTypeName typeName,
                            VerificationDeadlineEvent event) {

        Instant fireInstant = fireAt.atZone(ZoneId.systemDefault()).toInstant();

        if (fireInstant.isBefore(Instant.now())) {
            log.info("[인증 마감 알림] 예약 시각({})이 이미 지남 → 즉시 실행 | type={}", fireAt, typeName);
            sendNotification(event, typeName, fireAt.toLocalDate());
            return;
        }

        LocalDate targetDate = fireAt.toLocalDate();
        taskScheduler.schedule(
                () -> sendNotification(event, typeName, targetDate),
                fireInstant
        );
        log.info("[인증 마감 알림] 예약 완료 | RoundId={} | type={} | fireAt={}",
                event.roundId(), typeName, fireAt);
    }

    // 실제 알림 생성 & FCM 이벤트 발행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(VerificationDeadlineEvent event,
                                 NotificationTypeName typeName,
                                 LocalDate targetDate) {
        Long roundId = event.roundId();

        // 멱등성 체크: 같은 라운드 + 같은 타입 + 같은 날짜에 이미 생성됐으면 skip
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd   = targetDate.plusDays(1).atStartOfDay();

        if (notificationEventRepository.existsByContextTypeAndContextIdAndTypeTypeNameAndCreatedAtBetween(
                ResourceType.ROUND, roundId, typeName, dayStart, dayEnd)) {
            log.info("[인증 마감 알림] 당일 이미 발송된 알림 건너뜀 | RoundId={} | type={} | date={}",
                    roundId, typeName, targetDate);
            return;
        }

        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROUND_NOT_FOUND));
        Challenge challenge = round.getChallenge();

        NotificationType type = typeRepository.findByTypeName(typeName)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .type(type)
                .category(NotificationCategory.VERIFICATION)
                .targetType(ResourceType.CHALLENGE)
                .targetId(challenge.getId())
                .contextType(ResourceType.ROUND)
                .contextId(roundId)
                .title(buildTitle())
                .message(buildMessage(typeName, challenge.getTitle()))
                .imageKey(challenge.getImageKey())
                .build();
        notificationEventRepository.save(notificationEvent);

        List<RoundRecord> records = roundRecordRepository
                .findAllByRoundWithUserAndSetting(round, ChallengeJoinStatus.JOINED);

        List<NotificationDelivery> deliveries = records.stream()
                .filter(this::isVerificationEnabled)
                .map(r -> NotificationDelivery.builder()
                        .event(notificationEvent)
                        .receiver(r.getUserChallenge().getUser())
                        .isRead(false)
                        .build())
                .toList();

        if (deliveries.isEmpty()) {
            log.info("[인증 마감 알림] 수신 대상 없음 | RoundId={}", roundId);
            return;
        }

        notificationRepository.saveAll(deliveries);
        log.info("[인증 마감 알림] DB 저장 완료 | RoundId={} | type={} | date={} | 대상={}명",
                roundId, typeName, targetDate, deliveries.size());

        eventPublisher.publishEvent(new FcmPushSendEvent(deliveries, notificationEvent));
    }


    private String buildTitle() {
        return "완료되지 않은 인증이 있어요";
    }

    private String buildMessage(NotificationTypeName typeName, String challengeTitle) {
        return switch (typeName) {
            case VERIFICATION_DEADLINE_3H  -> String.format("[%s] 챌린지 인증 마감 시간이 3시간 남았어요", challengeTitle);
            case VERIFICATION_DEADLINE_1H  -> String.format("[%s] 챌린지 인증 마감 시간이 1시간 남았어요", challengeTitle);
            case VERIFICATION_DEADLINE_NOW -> String.format("[%s] 챌린지 인증 마감 시간이 1시간도 남지 않았어요", challengeTitle);
            default -> throw new IllegalArgumentException("지원하지 않는 알림 타입: " + typeName);
        };
    }

    // 유저의 인증 알림 설정 활성화 여부 확인
    private boolean isVerificationEnabled(RoundRecord record) {
        try {
            return record.getUserChallenge()
                    .getUser()
                    .getNotificationSetting()
                    .isVerificationEnabled();
        } catch (Exception e) {
            log.warn("[인증 마감 알림] 알림 설정 조회 실패, 기본값(true) 사용 | {}", e.getMessage());
            return true;
        }
    }
}