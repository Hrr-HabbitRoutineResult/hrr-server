package com.hrr.backend.domain.notification.listener;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.fcm.event.FcmPushSendEvent;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.notification.entity.NotificationType;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.event.VerificationDeadlineEvent;
import com.hrr.backend.domain.notification.repository.NotificationEventRepository;
import com.hrr.backend.domain.notification.repository.NotificationRepository;
import com.hrr.backend.domain.notification.repository.NotificationSettingRepository;
import com.hrr.backend.domain.notification.repository.NotificationTypeRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@RecordApplicationEvents
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VerificationDeadlineNotificationListenerTest {

    @Autowired private VerificationDeadlineNotificationListener listener;
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
    private VerificationDeadlineEvent testEvent;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();

        Challenge challenge = challengeRepository.save(Challenge.builder()
                .title("테스트 챌린지")
                .description("테스트 설명")
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
                .build());

        testRound = roundRepository.save(Round.builder()
                .challenge(challenge)
                .roundNumber(1)
                .startDate(today)
                .endDate(today.plusDays(7))
                .build());

        notificationTypeRepository.save(NotificationType.builder()
                .typeName(NotificationTypeName.VERIFICATION_DEADLINE_1H)
                .build());

        testEvent = new VerificationDeadlineEvent(testRound.getId(), challenge.getId(), today.atTime(9, 0), today.atTime(22, 0));
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
    @DisplayName("성공: 오늘 인증이 없는 유저에게만 알림 데이터가 DB에 저장된다")
    void sendNotification_WithRealData_Test() {
        // given
        User verifiedUser = createUser("verified_user");
        UserChallenge uc1 = joinChallenge(verifiedUser);
        RoundRecord rr1 = createRoundRecord(uc1);
        saveVerification(rr1, VerificationStatus.COMPLETED);

        User unverifiedUser = createUser("unverified_user");
        UserChallenge uc2 = joinChallenge(unverifiedUser);
        createRoundRecord(uc2);

        // when
        listener.sendNotification(testEvent, NotificationTypeName.VERIFICATION_DEADLINE_1H, LocalDate.now());

        // then
        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationEvent> events = notificationEventRepository.findAll();
            assertThat(events).isNotEmpty();

            List<NotificationDelivery> deliveries = notificationRepository.findAll();
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).getReceiver().getName()).isEqualTo("unverified_user");
        });

        // ApplicationEvents를 사용하여 FCM 이벤트 발행 여부 검증
        long eventCount = applicationEvents.stream(FcmPushSendEvent.class).count();
        assertThat(eventCount).isEqualTo(1);
    }

    @Test
    @DisplayName("실패: 유저가 미인증 상태여도 알림 설정을 껐다면 알림 데이터가 생성되지 않는다")
    void shouldNotCreateNotificationWhenSettingIsDisabled() {
        // given
        User user = userRepository.save(User.builder().name("disabled_user").nickname("off_nick").isPublic(true).build());

        // 알림 설정 OFF
        notificationSettingRepository.save(NotificationSetting.builder()
                .user(user)
                .isVerificationEnabled(false)
                .isBadgeEnabled(true).isChallengeEnabled(true).isFollowEnabled(true).build());

        UserChallenge uc = joinChallenge(user);
        createRoundRecord(uc);

        // when
        listener.sendNotification(testEvent, NotificationTypeName.VERIFICATION_DEADLINE_1H, LocalDate.now());

        // then
        List<NotificationDelivery> deliveries = notificationRepository.findAll();
        assertThat(deliveries).isEmpty(); // 알림이 생성되지 않아야 함
    }

    private User createUser(String name) {
        User user = userRepository.save(User.builder().name(name).nickname(name + "_nick").isPublic(true).build());
        notificationSettingRepository.save(NotificationSetting.builder()
                .user(user).isVerificationEnabled(true).isBadgeEnabled(true).isChallengeEnabled(true).isFollowEnabled(true).build());
        return user;
    }

    private UserChallenge joinChallenge(User user) {
        return userChallengeRepository.save(UserChallenge.builder()
                .user(user).challenge(testRound.getChallenge()).status(ChallengeJoinStatus.JOINED).build());
    }

    private RoundRecord createRoundRecord(UserChallenge uc) {
        return roundRecordRepository.save(RoundRecord.builder().round(testRound).userChallenge(uc).build());
    }

    private void saveVerification(RoundRecord rr, VerificationStatus status) {
        verificationRepository.save(Verification.builder()
                .roundRecord(rr).userChallenge(rr.getUserChallenge()).roundId(testRound.getId()).status(status).build());
    }
}