package com.hrr.backend.domain.notification.listener;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.fcm.event.FcmPushSendEvent; // 추가된 이벤트
import com.hrr.backend.domain.notification.entity.*;
import com.hrr.backend.domain.notification.entity.enums.*;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionResponseEvent;
import com.hrr.backend.domain.notification.repository.*;
import com.hrr.backend.domain.round.entity.*;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.repository.*;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher; // 주입 필요
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final RoundRepository roundRepository;
    private final RoundRecordRepository roundRecordRepository;
    private final NotificationTypeRepository typeRepository;
    private final NotificationEventRepository eventRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleChallengeExtensionEvent(ChallengeExtensionEvent event) {
        Long roundId = event.roundId();

        // 멱등성 체크
        if (eventRepository.existsByContextTypeAndContextIdAndCreatedAtAfter(
                ResourceType.ROUND, roundId, LocalDate.now().atStartOfDay())) {
            return;
        }

        // 데이터 조회
        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROUND_NOT_FOUND));
        Challenge challenge = round.getChallenge();

        NotificationType type = typeRepository.findByTypeName(NotificationTypeName.CHALLENGE_EXTENSION)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .type(type)
                .category(NotificationCategory.CHALLENGE)
                .targetType(ResourceType.CHALLENGE)
                .targetId(challenge.getId())
                .contextType(ResourceType.ROUND)
                .contextId(roundId)
                .title(String.format("%s 챌린지 종료 3일 전입니다.", challenge.getTitle()))
                .message("다음 라운드에도 참여하시겠어요?\n내일까지 연장 여부를 알려주세요!")
                .imageKey(challenge.getImageKey())
                .build();
        eventRepository.save(notificationEvent);

        // 수신자 전체에 대해 Delivery 생성
        List<RoundRecord> records = roundRecordRepository.findAllByRoundWithUserAndSetting(
                round,
                ChallengeJoinStatus.JOINED
        );

        List<NotificationDelivery> deliveries = records.stream()
                .map(record -> NotificationDelivery.builder()
                        .event(notificationEvent)
                        .receiver(record.getUserChallenge().getUser())
                        .isRead(false)
                        .build())
                .toList();

        if (!deliveries.isEmpty()) {
            notificationRepository.saveAll(deliveries);
            log.info("챌린지 연장 알림 DB 저장 완료: RoundId={}, 대상={}명", roundId, deliveries.size());

            // 직접 FCM을 호출하지 않고 이벤트를 발행
            eventPublisher.publishEvent(new FcmPushSendEvent(deliveries, notificationEvent));
        }
    }

    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleChallengeExtensionResponseEvent(ChallengeExtensionResponseEvent event) {
        Long roundId = event.roundId();
        User user = event.user();

        // 멱등성 체크
        List<NotificationTypeName> resultTypes = List.of(
                NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS,
                NotificationTypeName.CHALLENGE_EXTENSION_CANCEL
        );

        if (notificationRepository.existsResponseNotification(user, ResourceType.ROUND, roundId, resultTypes)) {
            log.info("이미 결과 알림(성공/취소)이 처리된 라운드입니다: User={}, Round={}", user.getId(), roundId);
            return;
        }

        Round round = roundRepository.findById(roundId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROUND_NOT_FOUND));
        Challenge challenge = round.getChallenge();

        // NextRoundIntent에 따른 알림 타입 및 메시지 결정
        NotificationTypeName typeName;
        String title;
        String message;

        if (event.intent() == NextRoundIntent.CONTINUE) {
            typeName = NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS;
            title = String.format("%s 챌린지가 연장되었어요", challenge.getTitle());
            message = "다음 라운드에서도 루틴을 이어가요";
        } else {
            typeName = NotificationTypeName.CHALLENGE_EXTENSION_CANCEL;
            title = String.format("%s 챌린지를 마무리해요", challenge.getTitle());
            message = "챌린지가 예정대로 종료돼요. 그동안 수고 많으셨어요";
        }

        NotificationType type = typeRepository.findByTypeName(typeName)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .type(type)
                .category(NotificationCategory.CHALLENGE)
                .targetType(ResourceType.CHALLENGE)
                .targetId(challenge.getId())
                .contextType(ResourceType.ROUND)
                .contextId(round.getId())
                .title(title)
                .message(message)
                .imageKey(challenge.getImageKey())
                .build();
        eventRepository.save(notificationEvent);

        NotificationDelivery delivery = NotificationDelivery.builder()
                .event(notificationEvent)
                .receiver(user)
                .isRead(false)
                .build();
        notificationRepository.save(delivery);

        log.info("연장 응답 알림 생성 완료: User={}, Intent={}", user.getNickname(), event.intent());

        // 내부 이벤트 발행 (DB 커밋 후 발송 보장)
        eventPublisher.publishEvent(new FcmPushSendEvent(List.of(delivery), notificationEvent));
    }
}