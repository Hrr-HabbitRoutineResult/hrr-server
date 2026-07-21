package com.hrr.backend.domain.notification.service;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.comment.repository.CommentRepository;
import com.hrr.backend.domain.fcm.event.FcmPushSendEvent;
import com.hrr.backend.domain.notification.entity.*;
import com.hrr.backend.domain.notification.entity.enums.*;
import com.hrr.backend.domain.notification.event.*;
import com.hrr.backend.domain.notification.repository.*;
import com.hrr.backend.domain.round.entity.*;
import com.hrr.backend.domain.round.repository.*;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
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

    private final ChallengeRepository challengeRepository;
    private final RoundRepository roundRepository;
    private final RoundRecordRepository roundRecordRepository;
    private final NotificationTypeRepository typeRepository;
    private final NotificationEventRepository eventRepository;
    private final NotificationRepository notificationRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final UserRepository userRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final VerificationRepository verificationRepository;
    private final CommentRepository commentRepository;
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
    // 알림 저장 실패가 원본 비즈니스 트랜잭션에 영향을 주지 않도록 별도 트랜잭션 사용
    public void sendCommentCreatedNotification(CommentCreatedEvent event) {
        Verification verification = verificationRepository.findById(event.verificationId())
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));
        Comment comment = commentRepository.findById(event.commentId())
                .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        User receiver = verification.getUserChallenge().getUser();
        // 자신의 인증에 직접 댓글을 작성한 경우 알림 미발송
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

        // 질문 작성자는 제외하고 같은 챌린지 참여자에게만 알림 생성
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
    public void sendChallengeStartNotification(ChallengeStartEvent event) {
        Long challengeId = event.challengeId();
        LocalDate today = LocalDate.now();

        // 멱등성 체크
        if (eventRepository.existsByContextTypeAndContextIdAndTypeTypeNameAndCreatedDate(
                ResourceType.CHALLENGE, challengeId, NotificationTypeName.CHALLENGE_START, today)) {
            return;
        }

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));
        NotificationType type = typeRepository.findByTypeName(NotificationTypeName.CHALLENGE_START)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));

        String title = String.format("%s D-1", challenge.getTitle());
        String message = String.format("내가 참여한 %s 챌린지가 내일 새로 시작해요", challenge.getTitle());
        NotificationEvent notificationEvent = createChallengeEvent(
                type, NotificationCategory.CHALLENGE, challenge, title, message, today);

        try {
            eventRepository.saveAndFlush(notificationEvent);
        } catch (DataIntegrityViolationException e) {
            log.info("중복된 챌린지 시작 알림 이벤트 생성이 차단되었습니다. (ChallengeId={})", challengeId);
            return;
        }

        List<UserChallenge> participants = userChallengeRepository.findAllByChallengeIdAndStatusWithUserAndSetting(
                challengeId, ChallengeJoinStatus.JOINED);

        // 내역 생성
        List<NotificationDelivery> allDeliveries = createDeliveriesForChallengeParticipants(participants, notificationEvent);

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
    public void sendChallengeUpdatedNotification(ChallengeUpdatedEvent event) {
        Long challengeId = event.challengeId();
        LocalDate today = LocalDate.now();

        // 멱등성 체크
        if (eventRepository.existsByContextTypeAndContextIdAndTypeTypeNameAndCreatedDate(
                ResourceType.CHALLENGE, challengeId, NotificationTypeName.CHALLENGE_UPDATED, today)) {
            return;
        }

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));
        NotificationType type = typeRepository.findByTypeName(NotificationTypeName.CHALLENGE_UPDATED)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));

        NotificationEvent notificationEvent = createChallengeEvent(
                type, NotificationCategory.CHALLENGE, challenge,
                challenge.getTitle(), "챌린지가 수정되었어요! 지금 확인해보세요.", today);

        try {
            eventRepository.saveAndFlush(notificationEvent);
        } catch (DataIntegrityViolationException e) {
            log.info("중복된 챌린지 수정 알림 이벤트 생성이 차단되었습니다. (ChallengeId={})", challengeId);
            return;
        }

        List<UserChallenge> participants = userChallengeRepository.findAllByChallengeIdAndStatusWithUserAndSetting(
                challengeId, ChallengeJoinStatus.JOINED);

        // 내역 생성
        List<NotificationDelivery> allDeliveries = createDeliveriesForChallengeParticipants(participants, notificationEvent);

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
    public void sendFollowCreatedNotification(FollowCreatedEvent event) {
        User actor = userRepository.findById(event.actor().getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        User receiver = userRepository.findById(event.receiver().getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));
        NotificationType type = typeRepository.findByTypeName(NotificationTypeName.FOLLOW_CREATED)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_TYPE_NOT_FOUND));
        LocalDate today = LocalDate.now();

        NotificationEvent notificationEvent = eventHelper.getOrCreateSharedEvent(
                ResourceType.USER, actor.getId(), type,
                "새로운 팔로워가 있어요",
                String.format("%s 님이 회원님을 팔로우하기 시작했어요", actor.getNickname()),
                today,
                () -> createFollowEvent(actor, type, today)
        );

        NotificationEvent mergedEvent = eventRepository.findById(notificationEvent.getId()).orElseThrow();

        NotificationDelivery delivery = NotificationDelivery.builder()
                .event(mergedEvent)
                .receiver(receiver)
                .isRead(false)
                .build();

        notificationRepository.save(delivery);

        if (receiver.getNotificationSetting().isFollowEnabled()) {
            eventPublisher.publishEvent(new FcmPushSendEvent(List.of(delivery), mergedEvent));
        }
    }

    private NotificationEvent createFollowEvent(User actor, NotificationType type, LocalDate createdDate) {
        return NotificationEvent.builder()
                .type(type)
                .actor(actor)
                .category(NotificationCategory.FOLLOW)
                .targetType(ResourceType.USER)
                .targetId(actor.getId())
                .contextType(ResourceType.USER)
                .contextId(actor.getId())
                .title("새로운 팔로워가 있어요")
                .message(String.format("%s 님이 회원님을 팔로우하기 시작했어요", actor.getNickname()))
                .imageKey(actor.getProfileImage())
                .createdDate(createdDate)
                .build();
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

    private NotificationEvent createChallengeEvent(NotificationType type, NotificationCategory category, Challenge challenge,
                                                   String title, String message, LocalDate createdDate) {
        return NotificationEvent.builder()
                .type(type)
                .category(category)
                .targetType(ResourceType.CHALLENGE)
                .targetId(challenge.getId())
                .contextType(ResourceType.CHALLENGE)
                .contextId(challenge.getId())
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

    private List<NotificationDelivery> createDeliveriesForChallengeParticipants(List<UserChallenge> participants, NotificationEvent event) {
        return participants.stream()
                .map(uc -> NotificationDelivery.builder()
                        .event(event)
                        .receiver(uc.getUser())
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
