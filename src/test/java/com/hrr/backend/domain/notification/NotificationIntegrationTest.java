package com.hrr.backend.domain.notification;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.fcm.event.FcmPushSendEvent;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.notification.entity.NotificationType;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionResponseEvent;
import com.hrr.backend.domain.notification.event.VerificationDeadlineEvent;
import com.hrr.backend.domain.notification.listener.NotificationEventListener;
import com.hrr.backend.domain.notification.listener.VerificationDeadlineNotificationListener;
import com.hrr.backend.domain.notification.repository.NotificationRepository;
import com.hrr.backend.domain.notification.repository.NotificationSettingRepository;
import com.hrr.backend.domain.notification.repository.NotificationTypeRepository;
import com.hrr.backend.domain.notification.repository.NotificationEventRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
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
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationIntegrationTest {

    @Autowired private VerificationDeadlineNotificationListener deadlineListener;
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

    @Autowired(required = false)
    private ApplicationEvents applicationEvents;

    private Round testRound;
    private Challenge testChallenge;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();

        // 챌린지 생성
        testChallenge = challengeRepository.save(Challenge.builder()
                .title("테스트 챌린지")
                .description("설명")
                .status(ChallengeStatus.ONGOING)
                .category(Category.HEALTH)
                .startDate(today.atStartOfDay())
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
                .challenge(testChallenge).roundNumber(1).startDate(today).endDate(today.plusDays(7)).build());

        // 모든 알림 타입 등록
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
    @DisplayName("[통합] 1. 인증 마감 알림: 오늘 인증이 없는 유저에게만 알림이 발송된다")
    void verificationDeadline_Integration_Test() {
        // given
        User verifiedUser = createUser("verified_user", true);
        saveVerification(createRoundRecord(joinChallenge(verifiedUser)), VerificationStatus.COMPLETED);

        User unverifiedUser = createUser("unverified_user", true);
        createRoundRecord(joinChallenge(unverifiedUser));

        verificationRepository.flush();

        VerificationDeadlineEvent event = new VerificationDeadlineEvent(testRound.getId(), testChallenge.getId(),
                LocalDate.now().atTime(9, 0), LocalDate.now().atTime(22, 0));

        // when
        deadlineListener.handleVerificationDeadlineEvent(event);

        // then
        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationDelivery> deliveries = notificationRepository.findAll();
            // 인증 완료자를 제외하고 1명만 있어야 함
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).getReceiver().getName()).isEqualTo("unverified_user");
        });
    }

    @Test
    @DisplayName("[통합] 2. 인증 마감 알림: 설정을 끈 유저는 제외된다")
    void verificationDeadline_DisabledSetting_Test() {
        // given
        User disabledUser = createUser("disabled_user", false); // 설정 OFF
        createRoundRecord(joinChallenge(disabledUser));
        roundRecordRepository.flush();

        VerificationDeadlineEvent event = new VerificationDeadlineEvent(testRound.getId(), testChallenge.getId(),
                LocalDate.now().atTime(9, 0), LocalDate.now().atTime(22, 0));

        // when
        deadlineListener.handleVerificationDeadlineEvent(event);

        // then
        List<NotificationDelivery> deliveries = notificationRepository.findAll();
        assertThat(deliveries).isEmpty();
    }

    @Test
    @DisplayName("[통합] 3. 챌린지 연장 안내: 참여 중인 모든 유저에게 알림이 저장된다")
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

        // then: @Async 처리를 위한 대기
        Thread.sleep(1500);

        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationDelivery> deliveries = notificationRepository.findAll();
            assertThat(deliveries).hasSize(2); // 두 유저 모두 받아야 함
        });
    }

    @Test
    @DisplayName("[통합] 4. 연장 응답 결과: 개별 유저에게 성공 알림이 발송된다")
    void challengeExtensionResponse_Integration_Test() throws InterruptedException {
        // given
        User user = createUser("responder", true);
        createRoundRecord(joinChallenge(user)); // 라운드 기록이 있어야 함
        roundRecordRepository.flush();

        ChallengeExtensionResponseEvent event = new ChallengeExtensionResponseEvent(testRound.getId(), user, NextRoundIntent.CONTINUE);

        // when
        eventListener.handleChallengeExtensionResponseEvent(event);

        // then: @Async 대기
        Thread.sleep(1500);

        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationDelivery> deliveries = notificationRepository.findAll();
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).getEvent().getType().getTypeName()).isEqualTo(NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS);
        });
    }

    // --- 헬퍼 메서드 ---
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