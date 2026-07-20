package com.hrr.backend.domain.notification.service;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.comment.repository.CommentRepository;
import com.hrr.backend.domain.fcm.event.FcmPushSendEvent;
import com.hrr.backend.domain.notification.entity.*;
import com.hrr.backend.domain.notification.entity.enums.*;
import com.hrr.backend.domain.notification.event.*;
import com.hrr.backend.domain.notification.repository.*;
import com.hrr.backend.domain.notification.service.helper.NotificationEventHelper;
import com.hrr.backend.domain.round.entity.*;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.repository.*;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
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
    private final UserRepository userRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final VerificationRepository verificationRepository;
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationEventHelper eventHelper;

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
                round, ChallengeJoinStatus.JOINED, VerificationStatus.COMPLETED, targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay()
        );

        // 내역 생성
        List<NotificationDelivery> allDeliveries = createDeliveries(records, notificationEvent);

        if (!allDeliveries.isEmpty()) {
            notificationRepository.saveAll(allDeliveries);

            // 푸시 발송만 설정 확인 후 진행
            List<NotificationDelivery> pushTargets = allDeliveries.stream()
                    .filter(d -> d.getReceiver().getNotificationSetting().isVerificationEnabled())
                    .toList();

            if (!pushTargets.isEmpty()) {
                eventPublisher.publishEvent(new FcmPushSendEvent(pushTargets, notificationEvent));
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendCommentCreatedNotification(CommentCreatedEvent event) {
        Verification verification = verificationRepository.findById(event.verificationId())
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));
        Comment comment = commentRepository.findById(event.commentId())
                .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        User receiver = verification.getUserChallenge().getUser();
        if (receiver.getId().equals(event.actorId())) {
            return;
        }

        NotificationType type = typeRepository.findByTypeName(NotificationTypeName.COMMENT_CREATED)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));
        Challenge challenge = verification.getRoundRecord().getRound().getChallenge();

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .type(type)
                .actor(comment.getUser())
                .category(NotificationCategory.VERIFICATION)
                .targetType(ResourceType.VERIFICATION)
                .targetId(verification.getId())
                .contextType(ResourceType.COMMENT)
                .contextId(comment.getId())
                .title("내 인증에 댓글이 달렸어요")
                .message(comment.getContent())
                .imageKey(challenge.getImageKey())
                .createdDate(LocalDate.now())
                .build();

        try {
            eventRepository.saveAndFlush(notificationEvent);
        } catch (DataIntegrityViolationException e) {
            log.info("중복된 댓글 생성 알림 이벤트 생성이 차단되었습니다. (CommentId={})", comment.getId());
            return;
        }

        NotificationDelivery delivery = NotificationDelivery.builder()
                .event(notificationEvent)
                .receiver(receiver)
                .isRead(false)
                .build();

        notificationRepository.save(delivery);

        if (receiver.getNotificationSetting().isVerificationEnabled()) {
            eventPublisher.publishEvent(new FcmPushSendEvent(List.of(delivery), notificationEvent));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendQuestionVerificationCreatedNotification(QuestionVerificationCreatedEvent event) {
        Verification verification = verificationRepository.findById(event.verificationId())
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        if (!Boolean.TRUE.equals(verification.getIsQuestion())) {
            return;
        }

        Challenge challenge = verification.getRoundRecord().getRound().getChallenge();
        User actor = verification.getUserChallenge().getUser();
        NotificationType type = typeRepository.findByTypeName(NotificationTypeName.QUESTION_VERIFICATION)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .type(type)
                .actor(actor)
                .category(NotificationCategory.VERIFICATION)
                .targetType(ResourceType.VERIFICATION)
                .targetId(verification.getId())
                .contextType(ResourceType.VERIFICATION)
                .contextId(verification.getId())
                .title("새로운 질문이 등록되었어요")
                .message(String.format("%s 챌린지에 새로운 질문 인증글이 등록되었어요", challenge.getTitle()))
                .imageKey(challenge.getImageKey())
                .createdDate(LocalDate.now())
                .build();

        try {
            eventRepository.saveAndFlush(notificationEvent);
        } catch (DataIntegrityViolationException e) {
            log.info("중복된 질문 인증 알림 이벤트 생성이 차단되었습니다. (VerificationId={})", verification.getId());
            return;
        }

        List<UserChallenge> joinedUserChallenges =
                userChallengeRepository.findAllByChallengeIdAndStatusWithUserAndSetting(
                        challenge.getId(), ChallengeJoinStatus.JOINED);

        List<NotificationDelivery> allDeliveries = joinedUserChallenges.stream()
                .map(UserChallenge::getUser)
                .filter(receiver -> !receiver.getId().equals(event.actorId()))
                .map(receiver -> NotificationDelivery.builder()
                        .event(notificationEvent)
                        .receiver(receiver)
                        .isRead(false)
                        .build())
                .toList();

        if (!allDeliveries.isEmpty()) {
            notificationRepository.saveAll(allDeliveries);

            List<NotificationDelivery> pushTargets = allDeliveries.stream()
                    .filter(d -> d.getReceiver().getNotificationSetting().isVerificationEnabled())
                    .toList();

            if (!pushTargets.isEmpty()) {
                eventPublisher.publishEvent(new FcmPushSendEvent(pushTargets, notificationEvent));
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendWeakVerificationWarningNotification(WeakVerificationWarningEvent event) {
        Verification verification = verificationRepository.findById(event.verificationId())
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));
        User receiver = userRepository.findById(event.warnedUserId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        Challenge challenge = verification.getRoundRecord().getRound().getChallenge();
        NotificationType type = typeRepository.findByTypeName(NotificationTypeName.WEAK_VERIFICATION_WARNING)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .type(type)
                .actor(null)
                .category(NotificationCategory.VERIFICATION)
                .targetType(ResourceType.VERIFICATION)
                .targetId(verification.getId())
                .contextType(ResourceType.VERIFICATION)
                .contextId(verification.getId())
                .title("챌린지 부실 인증 경고")
                .message(String.format("%s 챌린지에서 부실 인증 경고가 적립되었어요", challenge.getTitle()))
                .imageKey(challenge.getImageKey())
                .createdDate(LocalDate.now())
                .build();

        try {
            eventRepository.saveAndFlush(notificationEvent);
        } catch (DataIntegrityViolationException e) {
            log.info("중복된 부실 인증 경고 알림 이벤트 생성이 차단되었습니다. (VerificationId={}, WarnedUserId={})",
                    verification.getId(), receiver.getId());
            return;
        }

        NotificationDelivery delivery = NotificationDelivery.builder()
                .event(notificationEvent)
                .receiver(receiver)
                .isRead(false)
                .build();

        notificationRepository.save(delivery);

        if (receiver.getNotificationSetting().isVerificationEnabled()) {
            eventPublisher.publishEvent(new FcmPushSendEvent(List.of(delivery), notificationEvent));
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendChallengeExtensionNotification(ChallengeExtensionEvent event) {
        Long roundId = event.roundId();

        // 멱등성 체크
        if (eventRepository.existsByContextTypeAndContextIdAndTypeTypeNameAndCreatedDate(
                ResourceType.ROUND, roundId, NotificationTypeName.CHALLENGE_EXTENSION, LocalDate.now())) {
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

        // 내역 생성
        List<NotificationDelivery> allDeliveries = createDeliveries(records, notificationEvent);

        if (!allDeliveries.isEmpty()) {
            notificationRepository.saveAll(allDeliveries);

            // 푸시 발송만 설정 확인
            List<NotificationDelivery> pushTargets = allDeliveries.stream()
                    .filter(d -> d.getReceiver().getNotificationSetting().isChallengeEnabled())
                    .toList();

            if (!pushTargets.isEmpty()) {
                eventPublisher.publishEvent(new FcmPushSendEvent(pushTargets, notificationEvent));
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendChallengeExtensionResponseNotification(ChallengeExtensionResponseEvent event) {
        Long roundId = event.roundId();
        User user = userRepository.findById(event.user().getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        LocalDate today = LocalDate.now();

        // 멱등성 체크
        if (notificationRepository.existsResponseNotification(user, ResourceType.ROUND, roundId,
                List.of(NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS, NotificationTypeName.CHALLENGE_EXTENSION_CANCEL))) {
            return;
        }

        NotificationTypeName typeName = (event.intent() == NextRoundIntent.CONTINUE)
                ? NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS : NotificationTypeName.CHALLENGE_EXTENSION_CANCEL;

        Round round = roundRepository.findById(roundId).orElseThrow(() -> new GlobalException(ErrorCode.ROUND_NOT_FOUND));
        Challenge challenge = round.getChallenge();
        NotificationType type = typeRepository.findByTypeName(typeName).orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));

        String title = (typeName == NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS)
                ? String.format("%s 챌린지가 연장되었어요", challenge.getTitle()) : String.format("%s 챌린지를 마무리해요", challenge.getTitle());
        String message = (typeName == NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS)
                ? "다음 라운드에서도 루틴을 이어가요" : "챌린지가 예정대로 종료돼요. 그동안 수고 많으셨어요";

        NotificationEvent notificationEvent = eventHelper.getOrCreateSharedEvent(
                ResourceType.ROUND, roundId, type, title, message, today,
                () -> createEvent(type, NotificationCategory.CHALLENGE, challenge, roundId, title, message, today)
        );

        NotificationEvent mergedEvent = eventRepository.findById(notificationEvent.getId()).orElseThrow();

        NotificationDelivery delivery = NotificationDelivery.builder()
                .event(mergedEvent)
                .receiver(user)
                .isRead(false)
                .build();

        // 내역 저장
        notificationRepository.save(delivery);

        // 푸시 발송 여부만 결정
        if (user.getNotificationSetting().isChallengeEnabled()) {
            eventPublisher.publishEvent(new FcmPushSendEvent(List.of(delivery), mergedEvent));
        }
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

    private List<NotificationDelivery> createDeliveries(List<RoundRecord> records, NotificationEvent event) {
        return records.stream()
                .map(r -> NotificationDelivery.builder()
                        .event(event)
                        .receiver(r.getUserChallenge().getUser())
                        .isRead(false)
                        .build())
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
