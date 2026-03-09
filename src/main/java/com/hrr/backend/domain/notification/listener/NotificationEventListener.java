package com.hrr.backend.domain.notification.listener;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.fcm.service.FcmPushService;
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
    private final FcmPushService fcmPushService; // 추가

    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleChallengeExtensionEvent(ChallengeExtensionEvent event) {
        Long roundId = event.getRoundId();

        // 멱등성 체크 (중복 알림 방지)
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

            // FCM 푸시 발송 (알림 설정 체크 포함)
            fcmPushService.sendPushForDeliveries(deliveries, notificationEvent);
            log.info("챌린지 연장 FCM 푸시 발송 요청 완료: RoundId={}", roundId);
        }
    }

    @Async("getAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleChallengeExtensionResponseEvent(ChallengeExtensionResponseEvent event) {
        Long roundId = event.getRoundId();
        User user = event.getUser();

        // 멱등성 체크
        List<NotificationTypeName> resultTypes = List.of(
                NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS,
                NotificationTypeName.CHALLENGE_EXTENSION_CANCEL
        );

        if (notificationRepository.existsResponseNotification(user, ResourceType.ROUND, roundId, resultTypes)) {
            log.info("이미 결과 알림(성공/취소)이 처리된 라운드입니다: User={}, Round={}", user.getId(), roundId);
            return;
        }

        Round round = roundRepository.findById(event.getRoundId())
                .orElseThrow(() -> new GlobalException(ErrorCode.ROUND_NOT_FOUND));
        Challenge challenge = round.getChallenge();

        // NextRoundIntent에 따른 알림 타입 및 메시지 결정
        NotificationTypeName typeName;
        String title;
        String message;

        if (event.getIntent() == NextRoundIntent.CONTINUE) {
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
                .receiver(event.getUser())
                .isRead(false)
                .build();
        notificationRepository.save(delivery);

        log.info("연장 응답 알림 생성 완료: User={}, Intent={}", event.getUser().getNickname(), event.getIntent());

        // FCM 푸시 발송 (알림 설정 체크 포함)
        fcmPushService.sendPushForDelivery(delivery, notificationEvent);
        log.info("연장 응답 FCM 푸시 발송 요청 완료: User={}", event.getUser().getNickname());
    }
}