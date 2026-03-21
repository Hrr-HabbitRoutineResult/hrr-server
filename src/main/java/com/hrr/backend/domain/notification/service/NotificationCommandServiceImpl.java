package com.hrr.backend.domain.notification.service;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.fcm.event.FcmPushSendEvent;
import com.hrr.backend.domain.notification.entity.*;
import com.hrr.backend.domain.notification.entity.enums.*;
import com.hrr.backend.domain.notification.event.*;
import com.hrr.backend.domain.notification.repository.*;
import com.hrr.backend.domain.round.entity.*;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.repository.*;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandServiceImpl implements NotificationCommandService {

    private final RoundRepository roundRepository;
    private final RoundRecordRepository roundRecordRepository;
    private final NotificationTypeRepository typeRepository;
    private final NotificationEventRepository eventRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendDeadlineNotification(VerificationDeadlineEvent event, NotificationTypeName typeName, LocalDate targetDate) {
        Long roundId = event.roundId();
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.plusDays(1).atStartOfDay();

        // 멱등성 체크
        if (eventRepository.existsByContextTypeAndContextIdAndTypeTypeNameAndCreatedDate(
                ResourceType.ROUND, roundId, typeName, targetDate)) {
            return;
        }

        Round round = roundRepository.findById(roundId).orElseThrow(() -> new GlobalException(ErrorCode.ROUND_NOT_FOUND));
        Challenge challenge = round.getChallenge();
        NotificationType type = typeRepository.findByTypeName(typeName).orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));

        // 알림 이벤트 생성
        NotificationEvent notificationEvent = createEvent(type, NotificationCategory.VERIFICATION, challenge, roundId, "완료되지 않은 인증이 있어요", buildDeadlineMessage(typeName, challenge.getTitle()), targetDate);

        try {
            eventRepository.saveAndFlush(notificationEvent);
        } catch (DataIntegrityViolationException e) {
            log.info("중복된 인증 마감 알림 이벤트 생성이 차단되었습니다. (RoundId={}, Type={})", roundId, typeName);
            return;
        }

        List<RoundRecord> records = roundRecordRepository.findAllByRoundAndNotVerifiedToday(
                round,
                ChallengeJoinStatus.JOINED,
                VerificationStatus.COMPLETED,
                dayStart,
                dayEnd
        );

        // 알림 설정 확인 및 발송
        List<NotificationDelivery> deliveries = createDeliveries(records, notificationEvent, true);

        if (!deliveries.isEmpty()) {
            notificationRepository.saveAll(deliveries);
            eventPublisher.publishEvent(new FcmPushSendEvent(deliveries, notificationEvent));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendChallengeExtensionNotification(ChallengeExtensionEvent event) {
        Long roundId = event.roundId();

        // 멱등성 체크
        if (eventRepository.existsByContextTypeAndContextIdAndCreatedDateGreaterThanEqual(
                ResourceType.ROUND, roundId, LocalDate.now())) {
            return;
        }

        Round round = roundRepository.findById(roundId).orElseThrow(() -> new GlobalException(ErrorCode.ROUND_NOT_FOUND));
        Challenge challenge = round.getChallenge();
        NotificationType type = typeRepository.findByTypeName(NotificationTypeName.CHALLENGE_EXTENSION).orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));

        NotificationEvent notificationEvent = createEvent(type, NotificationCategory.CHALLENGE, challenge, roundId,
                String.format("%s 챌린지 종료 3일 전입니다.", challenge.getTitle()), "다음 라운드에도 참여하시겠어요?\n내일까지 연장 여부를 알려주세요!", LocalDate.now());

        try {
            eventRepository.saveAndFlush(notificationEvent);
        } catch (DataIntegrityViolationException e) {
            log.info("중복된 챌린지 연장 안내 이벤트 생성이 차단되었습니다. (RoundId={})", roundId);
            return;
        }

        List<RoundRecord> records = roundRecordRepository.findAllByRoundWithUserAndSetting(round, ChallengeJoinStatus.JOINED);
        List<NotificationDelivery> deliveries = createDeliveries(records, notificationEvent, false);

        if (!deliveries.isEmpty()) {
            notificationRepository.saveAll(deliveries);
            eventPublisher.publishEvent(new FcmPushSendEvent(deliveries, notificationEvent));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendChallengeExtensionResponseNotification(ChallengeExtensionResponseEvent event) {
        Long roundId = event.roundId();
        User user = event.user();

        // 멱등성 체크
        if (notificationRepository.existsResponseNotification(user, ResourceType.ROUND, roundId,
                List.of(NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS, NotificationTypeName.CHALLENGE_EXTENSION_CANCEL))) {
            return;
        }

        Round round = roundRepository.findById(roundId).orElseThrow(() -> new GlobalException(ErrorCode.ROUND_NOT_FOUND));
        Challenge challenge = round.getChallenge();

        NotificationTypeName typeName = (event.intent() == NextRoundIntent.CONTINUE)
                ? NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS : NotificationTypeName.CHALLENGE_EXTENSION_CANCEL;

        String title = (typeName == NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS)
                ? String.format("%s 챌린지가 연장되었어요", challenge.getTitle()) : String.format("%s 챌린지를 마무리해요", challenge.getTitle());

        String message = (typeName == NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS)
                ? "다음 라운드에서도 루틴을 이어가요" : "챌린지가 예정대로 종료돼요. 그동안 수고 많으셨어요";

        NotificationType type = typeRepository.findByTypeName(typeName).orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));

        NotificationEvent notificationEvent = createEvent(type, NotificationCategory.CHALLENGE, challenge, roundId,
                title, message, LocalDate.now());
        eventRepository.saveAndFlush(notificationEvent);

        NotificationDelivery delivery = NotificationDelivery.builder().event(notificationEvent).receiver(user).isRead(false).build();
        notificationRepository.save(delivery);

        eventPublisher.publishEvent(new FcmPushSendEvent(List.of(delivery), notificationEvent));
    }


    private NotificationEvent createEvent(NotificationType type, NotificationCategory category, Challenge challenge,
                                          Long contextId, String title, String message, LocalDate createdDate) {
        return NotificationEvent.builder()
                .type(type)
                .category(category)
                .targetType(ResourceType.CHALLENGE)
                .targetId(challenge.getId())
                .contextType(ResourceType.ROUND)
                .contextId(contextId)
                .title(title)
                .message(message)
                .imageKey(challenge.getImageKey())
                .createdDate(createdDate)
                .build();
    }

    private List<NotificationDelivery> createDeliveries(List<RoundRecord> records, NotificationEvent event, boolean checkVerificationSetting) {
        return records.stream()
                .filter(r -> !checkVerificationSetting || r.getUserChallenge().getUser().getNotificationSetting().isVerificationEnabled())
                .map(r -> NotificationDelivery.builder().event(event).receiver(r.getUserChallenge().getUser()).isRead(false).build())
                .toList();
    }

    private String buildDeadlineMessage(NotificationTypeName typeName, String title) {
        return switch (typeName) {
            case VERIFICATION_DEADLINE_3H -> String.format("[%s] 챌린지 인증 마감 3시간 전입니다.", title);
            case VERIFICATION_DEADLINE_1H -> String.format("[%s] 챌린지 인증 마감 1시간 전입니다.", title);
            case VERIFICATION_DEADLINE_NOW -> String.format("[%s] 챌린지 인증 마감이 임박했습니다.", title);
            default -> "인증 마감 시간이 다가오고 있어요.";
        };
    }
}