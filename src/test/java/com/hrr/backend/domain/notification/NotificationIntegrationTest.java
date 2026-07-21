package com.hrr.backend.domain.notification;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.fcm.event.FcmPushSendEvent;
import com.hrr.backend.domain.notification.entity.*;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.entity.enums.ResourceType;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeStartEvent;
import com.hrr.backend.domain.notification.event.ChallengeUpdatedEvent;
import com.hrr.backend.domain.notification.listener.NotificationEventListener;
import com.hrr.backend.domain.notification.repository.*;
import com.hrr.backend.domain.notification.service.NotificationCommandService;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import com.hrr.backend.global.scheduler.NotificationScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RecordApplicationEvents
class NotificationIntegrationTest {

    @Autowired private NotificationScheduler notificationScheduler;
    @Autowired private NotificationCommandService notificationCommandService;
    @Autowired private NotificationEventListener eventListener;
    @Autowired private RoundRecordRepository roundRecordRepository;
    @Autowired private RoundRepository roundRepository;
    @Autowired private ChallengeRepository challengeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserChallengeRepository userChallengeRepository;
    @Autowired private VerificationRepository verificationRepository;
    @Autowired private NotificationEventRepository notificationEventRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationTypeRepository notificationTypeRepository;
    @Autowired private NotificationSettingRepository notificationSettingRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private ApplicationEvents applicationEvents;

    private final LocalDate FIXED_DATE = LocalDate.of(2026, 3, 22);
    private final LocalDateTime FIXED_NOW = FIXED_DATE.atTime(10, 0);

    private Round testRound;
    private Challenge testChallenge;

    @BeforeEach
    void setUp() {
        testChallenge = challengeRepository.save(Challenge.builder()
                .title("테스트 챌린지")
                .description("설명")
                .status(ChallengeStatus.ONGOING)
                .category(Category.HEALTH)
                .startDate(FIXED_DATE.atStartOfDay())
                .verificationType(VerificationType.TEXT)
                .verifyStartTime(LocalTime.of(9, 0))
                .verifyEndTime(LocalTime.of(22, 0))
                .currentParticipants(0)
                .maxParticipants(10)
                .isPublic(true)
                .isViewerMode(false)
                .imageKey("test-image-key")
                .build());

        testRound = roundRepository.save(Round.builder()
                .challenge(testChallenge).roundNumber(1)
                .startDate(FIXED_DATE).endDate(FIXED_DATE.plusDays(7)).build());

        List<NotificationType> types = Arrays.stream(NotificationTypeName.values())
                .map(name -> NotificationType.builder().typeName(name).build())
                .toList();
        notificationTypeRepository.saveAll(types);
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        notificationEventRepository.deleteAll();
        notificationTypeRepository.deleteAll();
        notificationSettingRepository.deleteAll();
        verificationRepository.deleteAll();
        roundRecordRepository.deleteAll();
        roundRepository.deleteAll();
        userChallengeRepository.deleteAll();
        challengeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. 인증 마감 알림: 인증 윈도우가 1시간 미만이면 DEADLINE_NOW 알림이 발송된다")
    void verificationDeadline_SingleNotification_Test() {
        // given
        User unverifiedUser = createUser("unverified_user", true);
        createRoundRecord(joinChallenge(unverifiedUser));
        roundRecordRepository.flush();

        // 인증 윈도우 30분 — 즉시 발송 유도
        LocalDateTime startAt = FIXED_NOW.minusMinutes(10);
        LocalDateTime endAt = FIXED_NOW.plusMinutes(20);

        // when: 서비스 직접 호출
        notificationCommandService.sendDeadlineNotification(
                new com.hrr.backend.domain.notification.event.VerificationDeadlineEvent(
                        testRound.getId(), testChallenge.getId(), startAt, endAt),
                NotificationTypeName.VERIFICATION_DEADLINE_NOW,
                FIXED_DATE
        );

        // then
        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationDelivery> deliveries = notificationRepository.findAll();
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).getEvent().getType().getTypeName())
                    .isEqualTo(NotificationTypeName.VERIFICATION_DEADLINE_NOW);
        });
    }

    @Test
    @DisplayName("2. 인증 마감 알림: 설정을 끈 유저도 내역은 저장되지만 푸시 대상에서는 제외된다")
    void verificationDeadline_DisabledSetting_Test() {
        // given
        User disabledUser = createUser("disabled_user", false);
        createRoundRecord(joinChallenge(disabledUser));
        roundRecordRepository.flush();

        LocalDateTime startAt = FIXED_NOW.minusMinutes(10);
        LocalDateTime endAt = FIXED_NOW.plusMinutes(20);

        // when
        notificationCommandService.sendDeadlineNotification(
                new com.hrr.backend.domain.notification.event.VerificationDeadlineEvent(
                        testRound.getId(), testChallenge.getId(), startAt, endAt),
                NotificationTypeName.VERIFICATION_DEADLINE_NOW,
                FIXED_DATE
        );

        // then: 푸시 설정 꺼도 DB 내역은 생성됨
        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationDelivery> deliveries = notificationRepository.findAll();
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).getReceiver().getName()).isEqualTo("disabled_user");
        });
    }

    @Test
    @DisplayName("3. 챌린지 연장 안내: 참여 중인 모든 유저에게 알림이 저장된다")
    void challengeExtension_Integration_Test() throws InterruptedException {
        // given
        User u1 = createUser("user1", true);
        User u2 = createUser("user2", true);
        createRoundRecord(joinChallenge(u1));
        createRoundRecord(joinChallenge(u2));
        roundRecordRepository.flush();

        ChallengeExtensionEvent event = new ChallengeExtensionEvent(testRound.getId());

        // when
        eventListener.handleChallengeExtensionEvent(event);
        Thread.sleep(1500);

        // then
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(notificationRepository.findAll()).hasSize(2);
        });
    }

    @Test
    @DisplayName("4. 챌린지 시작 하루 전 알림: 참여자 전원에게 내역을 저장하고 FCM 이벤트를 발행한다")
    void challengeStart_Integration_Test() {
        // given
        User u1 = createUser("start_user1", true);
        User u2 = createUser("start_user2", true);
        joinChallenge(u1);
        joinChallenge(u2);
        userChallengeRepository.flush();

        ChallengeStartEvent event = new ChallengeStartEvent(testChallenge.getId());

        // when
        notificationCommandService.sendChallengeStartNotification(event);

        // then
        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationEvent> events = notificationEventRepository.findAll();
            List<NotificationDelivery> deliveries = notificationRepository.findAll();

            assertThat(events).hasSize(1);
            NotificationEvent notificationEvent = events.get(0);
            assertThat(notificationEvent.getCategory()).isEqualTo(NotificationCategory.CHALLENGE);
            assertThat(notificationEvent.getType().getTypeName()).isEqualTo(NotificationTypeName.CHALLENGE_START);
            assertThat(notificationEvent.getTargetType()).isEqualTo(ResourceType.CHALLENGE);
            assertThat(notificationEvent.getTargetId()).isEqualTo(testChallenge.getId());
            assertThat(notificationEvent.getContextType()).isEqualTo(ResourceType.CHALLENGE);
            assertThat(notificationEvent.getContextId()).isEqualTo(testChallenge.getId());
            assertThat(notificationEvent.getTitle()).isEqualTo("테스트 챌린지 D-1");
            assertThat(notificationEvent.getMessage()).isEqualTo("내가 참여한 테스트 챌린지 챌린지가 내일 새로 시작해요");
            assertThat(notificationEvent.getImageKey()).isEqualTo("test-image-key");
            assertThat(notificationEvent.getCreatedDate()).isEqualTo(LocalDate.now());

            assertThat(deliveries).hasSize(2);
            assertThat(deliveries)
                    .extracting(delivery -> delivery.getEvent().getId())
                    .containsOnly(notificationEvent.getId());
        });

        List<FcmPushSendEvent> pushEvents = applicationEvents.stream(FcmPushSendEvent.class).toList();
        assertThat(pushEvents).hasSize(1);
        assertThat(pushEvents.get(0).deliveries()).hasSize(2);
        assertThat(pushEvents.get(0).notificationEvent().getType().getTypeName())
                .isEqualTo(NotificationTypeName.CHALLENGE_START);
    }

    @Test
    @DisplayName("5. 챌린지 시작 하루 전 알림: 설정 OFF 유저도 내역은 저장되고 FCM은 발행되지 않는다")
    void challengeStart_DisabledSetting_Test() {
        // given
        User disabledUser = createUser("start_disabled_user", false);
        joinChallenge(disabledUser);
        userChallengeRepository.flush();

        ChallengeStartEvent event = new ChallengeStartEvent(testChallenge.getId());

        // when
        notificationCommandService.sendChallengeStartNotification(event);

        // then
        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationDelivery> deliveries = notificationRepository.findAll();
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).getReceiver().getName()).isEqualTo("start_disabled_user");
            assertThat(deliveries.get(0).getEvent().getType().getTypeName())
                    .isEqualTo(NotificationTypeName.CHALLENGE_START);
        });

        assertThat(applicationEvents.stream(FcmPushSendEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("6. 챌린지 수정 알림: 참여자 전원에게 내역을 저장하고 FCM 이벤트를 발행한다")
    void challengeUpdated_Integration_Test() {
        // given
        User u1 = createUser("updated_user1", true);
        User u2 = createUser("updated_user2", true);
        joinChallenge(u1);
        joinChallenge(u2);
        userChallengeRepository.flush();

        ChallengeUpdatedEvent event = new ChallengeUpdatedEvent(testChallenge.getId());

        // when
        notificationCommandService.sendChallengeUpdatedNotification(event);

        // then
        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationEvent> events = notificationEventRepository.findAll();
            List<NotificationDelivery> deliveries = notificationRepository.findAll();

            assertThat(events).hasSize(1);
            NotificationEvent notificationEvent = events.get(0);
            assertThat(notificationEvent.getCategory()).isEqualTo(NotificationCategory.CHALLENGE);
            assertThat(notificationEvent.getType().getTypeName()).isEqualTo(NotificationTypeName.CHALLENGE_UPDATED);
            assertThat(notificationEvent.getTargetType()).isEqualTo(ResourceType.CHALLENGE);
            assertThat(notificationEvent.getTargetId()).isEqualTo(testChallenge.getId());
            assertThat(notificationEvent.getContextType()).isEqualTo(ResourceType.CHALLENGE);
            assertThat(notificationEvent.getContextId()).isEqualTo(testChallenge.getId());
            assertThat(notificationEvent.getTitle()).isEqualTo("테스트 챌린지");
            assertThat(notificationEvent.getMessage()).isEqualTo("챌린지가 수정되었어요! 지금 확인해보세요.");
            assertThat(notificationEvent.getImageKey()).isEqualTo("test-image-key");
            assertThat(notificationEvent.getCreatedDate()).isEqualTo(LocalDate.now());

            assertThat(deliveries).hasSize(2);
            assertThat(deliveries)
                    .extracting(delivery -> delivery.getEvent().getId())
                    .containsOnly(notificationEvent.getId());
        });

        List<FcmPushSendEvent> pushEvents = applicationEvents.stream(FcmPushSendEvent.class).toList();
        assertThat(pushEvents).hasSize(1);
        assertThat(pushEvents.get(0).deliveries()).hasSize(2);
        assertThat(pushEvents.get(0).notificationEvent().getType().getTypeName())
                .isEqualTo(NotificationTypeName.CHALLENGE_UPDATED);
    }

    @Test
    @DisplayName("7. 챌린지 수정 알림: 설정 OFF 유저도 내역은 저장되고 FCM은 발행되지 않는다")
    void challengeUpdated_DisabledSetting_Test() {
        // given
        User disabledUser = createUser("updated_disabled_user", false);
        joinChallenge(disabledUser);
        userChallengeRepository.flush();

        ChallengeUpdatedEvent event = new ChallengeUpdatedEvent(testChallenge.getId());

        // when
        notificationCommandService.sendChallengeUpdatedNotification(event);

        // then
        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationDelivery> deliveries = notificationRepository.findAll();
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).getReceiver().getName()).isEqualTo("updated_disabled_user");
            assertThat(deliveries.get(0).getEvent().getType().getTypeName())
                    .isEqualTo(NotificationTypeName.CHALLENGE_UPDATED);
        });

        assertThat(applicationEvents.stream(FcmPushSendEvent.class)).isEmpty();
    }

    @Test
    @DisplayName("8. 멱등성 검증: 동일한 알림을 두 번 호출해도 한 번만 생성된다")
    void notificationIdempotency_Test() {
        // given
        User user = createUser("idempotent_user", true);
        createRoundRecord(joinChallenge(user));
        roundRecordRepository.flush();

        LocalDateTime startAt = FIXED_NOW.minusMinutes(10);
        LocalDateTime endAt = FIXED_NOW.plusMinutes(20);
        var event = new com.hrr.backend.domain.notification.event.VerificationDeadlineEvent(
                testRound.getId(), testChallenge.getId(), startAt, endAt);

        // when: 5분 폴링이 두 번 실행된 상황 시뮬레이션
        notificationCommandService.sendDeadlineNotification(event, NotificationTypeName.VERIFICATION_DEADLINE_NOW, FIXED_DATE);
        notificationCommandService.sendDeadlineNotification(event, NotificationTypeName.VERIFICATION_DEADLINE_NOW, FIXED_DATE);

        // then
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(notificationEventRepository.findAll()).hasSize(1);
            assertThat(notificationRepository.findAll()).hasSize(1);
        });
    }

    @Test
    @DisplayName("9. 다중 알림 검증: 3H 구간과 1H 구간에 각각 호출하면 알림이 각각 1개씩 생성된다")
    void multipleVerificationDeadlines_Test() {
        // given
        User user = createUser("multi_user", true);
        createRoundRecord(joinChallenge(user));
        roundRecordRepository.flush();

        // 마감 22:00 기준
        LocalDateTime startAt = FIXED_DATE.atTime(9, 0);
        LocalDateTime endAt = FIXED_DATE.atTime(22, 0);
        var event = new com.hrr.backend.domain.notification.event.VerificationDeadlineEvent(
                testRound.getId(), testChallenge.getId(), startAt, endAt);

        // when: 폴링 스케줄러가 3H 구간(19:00~21:00)과 1H 구간(21:00~22:00)에 각각 실행된 상황 시뮬레이션
        notificationCommandService.sendDeadlineNotification(event, NotificationTypeName.VERIFICATION_DEADLINE_3H, FIXED_DATE);
        notificationCommandService.sendDeadlineNotification(event, NotificationTypeName.VERIFICATION_DEADLINE_1H, FIXED_DATE);

        // then: 타입이 다르므로 Event 2개, Delivery 2개 생성
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(notificationEventRepository.findAll()).hasSize(2);
            assertThat(notificationRepository.findAll()).hasSize(2);
        });
    }

    private User createUser(String name, boolean enabled) {
        User user = userRepository.save(User.builder().name(name).nickname(name + "_nick").isPublic(true).build());
        notificationSettingRepository.save(NotificationSetting.builder()
                .user(user).isVerificationEnabled(enabled).isChallengeEnabled(enabled).isFollowEnabled(enabled).isBadgeEnabled(enabled).build());
        return user;
    }

    private UserChallenge joinChallenge(User user) {
        return userChallengeRepository.save(UserChallenge.builder().user(user).challenge(testChallenge).status(ChallengeJoinStatus.JOINED).build());
    }

    private RoundRecord createRoundRecord(UserChallenge uc) {
        return roundRecordRepository.save(RoundRecord.builder().round(testRound).userChallenge(uc).build());
    }

    private void saveVerification(RoundRecord rr, VerificationStatus status) {
        verificationRepository.save(Verification.builder().roundRecord(rr).userChallenge(rr.getUserChallenge())
                .roundId(testRound.getId()).status(status).build());
    }
}
