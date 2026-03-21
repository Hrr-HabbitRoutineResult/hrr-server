package com.hrr.backend.domain.notification;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.notification.entity.*;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionResponseEvent;
import com.hrr.backend.domain.notification.event.VerificationDeadlineEvent;
import com.hrr.backend.domain.notification.listener.NotificationEventListener;
import com.hrr.backend.domain.notification.listener.VerificationDeadlineNotificationListener;
import com.hrr.backend.domain.notification.repository.*;
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

    private Round testRound;
    private Challenge testChallenge;

    @BeforeEach
    void setUp() {
        LocalDate today = LocalDate.now();

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
    @DisplayName("1. 인증 마감 알림: 인증 시간이 짧을 때 단일 알림(NOW)만 즉시 발송된다")
    void verificationDeadline_SingleNotification_Test() {
        // given
        User verifiedUser = createUser("verified_user", true);
        saveVerification(createRoundRecord(joinChallenge(verifiedUser)), VerificationStatus.COMPLETED);

        User unverifiedUser = createUser("unverified_user", true);
        createRoundRecord(joinChallenge(unverifiedUser));
        verificationRepository.flush();

        // 예약 시각을 과거로 설정(now - 1분)하여 스케줄링 없이 즉시 실행을 유도
        LocalDateTime now = LocalDateTime.now();
        VerificationDeadlineEvent event = new VerificationDeadlineEvent(testRound.getId(), testChallenge.getId(),
                now.minusMinutes(1), now.plusMinutes(10));

        // when
        deadlineListener.handleVerificationDeadlineEvent(event);

        // then
        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationDelivery> deliveries = notificationRepository.findAll();
            // 인증 완료자를 제외하고 1명만 있어야 함
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).getReceiver().getName()).isEqualTo("unverified_user");
            assertThat(deliveries.get(0).getEvent().getType().getTypeName())
                    .isEqualTo(NotificationTypeName.VERIFICATION_DEADLINE_NOW);
        });
    }

    @Test
    @DisplayName("2. 인증 마감 알림: 설정을 끈 유저는 제외된다")
    void verificationDeadline_DisabledSetting_Test() {
        // given
        User disabledUser = createUser("disabled_user", false);
        createRoundRecord(joinChallenge(disabledUser));
        roundRecordRepository.flush();

        LocalDateTime now = LocalDateTime.now();
        VerificationDeadlineEvent event = new VerificationDeadlineEvent(testRound.getId(), testChallenge.getId(),
                now.minusMinutes(1), now.plusMinutes(10));

        // when
        deadlineListener.handleVerificationDeadlineEvent(event);

        // then: 알림 설정이 꺼져있으므로 내역이 없어야 함
        assertThat(notificationRepository.findAll()).isEmpty();
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
        Thread.sleep(1500); // @Async 처리 대기

        // then
        transactionTemplate.executeWithoutResult(status -> {
            assertThat(notificationRepository.findAll()).hasSize(2);
        });
    }

    @Test
    @DisplayName("4. 연장 응답 결과: 개별 유저에게 성공 알림이 발송된다")
    void challengeExtensionResponse_Integration_Test() throws InterruptedException {
        // given
        User user = createUser("responder", true);
        createRoundRecord(joinChallenge(user));
        roundRecordRepository.flush();

        ChallengeExtensionResponseEvent event = new ChallengeExtensionResponseEvent(testRound.getId(), user, NextRoundIntent.CONTINUE);

        // when
        eventListener.handleChallengeExtensionResponseEvent(event);
        Thread.sleep(1500);

        // then
        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationDelivery> deliveries = notificationRepository.findAll();
            assertThat(deliveries).hasSize(1);
            assertThat(deliveries.get(0).getEvent().getType().getTypeName())
                    .isEqualTo(NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS);
        });
    }

    @Test
    @DisplayName("5. 멱등성 검증: 동일한 알림을 두 번 호출해도 한 번만 생성된다")
    void notificationIdempotency_Test() {
        // given
        User user = createUser("idempotent_user", true);
        createRoundRecord(joinChallenge(user));
        roundRecordRepository.flush();

        LocalDateTime now = LocalDateTime.now();
        VerificationDeadlineEvent event = new VerificationDeadlineEvent(testRound.getId(), testChallenge.getId(),
                now.minusMinutes(1), now.plusMinutes(10));

        // when
        deadlineListener.handleVerificationDeadlineEvent(event); // 첫 번째 호출
        deadlineListener.handleVerificationDeadlineEvent(event); // 두 번째 호출 (중복 방지 작동해야 함)

        // then
        transactionTemplate.executeWithoutResult(status -> {
            // DB 유니크 제약 조건과 exists 체크 덕분에 1개만 생성됨
            assertThat(notificationEventRepository.findAll()).hasSize(1);
            assertThat(notificationRepository.findAll()).hasSize(1);
        });
    }

    @Test
    @DisplayName("6. 다중 알림 검증: 인증 시간이 3시간 이상이면 3H, 1H 알림이 모두 생성된다")
    void multipleVerificationDeadlines_Test() {
        // given
        User user = createUser("multi_user", true);
        createRoundRecord(joinChallenge(user));
        roundRecordRepository.flush();

        // 4시간으로 설정하여 3H, 1H 알림 조건 충족
        LocalDateTime now = LocalDateTime.now();
        VerificationDeadlineEvent event = new VerificationDeadlineEvent(testRound.getId(), testChallenge.getId(),
                now.minusHours(5), now.minusHours(1));

        // when
        deadlineListener.handleVerificationDeadlineEvent(event);

        // then
        transactionTemplate.executeWithoutResult(status -> {
            List<NotificationEvent> events = notificationEventRepository.findAll();
            // 3H와 1H 두 종류가 생성
            assertThat(events).hasSize(2);
            assertThat(notificationRepository.findAll()).hasSize(2);

            List<NotificationTypeName> typeNames = events.stream()
                    .map(e -> e.getType().getTypeName()).toList();
            assertThat(typeNames).containsExactlyInAnyOrder(
                    NotificationTypeName.VERIFICATION_DEADLINE_3H,
                    NotificationTypeName.VERIFICATION_DEADLINE_1H
            );
        });
    }

    @Test
    @DisplayName("7. 연장 응답 다중 사용자: 여러 명이 응답해도 Event는 1개만 생성되고 Delivery는 각각 생성된다")
    void challengeExtensionResponse_MultipleUsers_Test() throws InterruptedException {
        // given
        User user1 = createUser("user1", true);
        User user2 = createUser("user2", true);
        createRoundRecord(joinChallenge(user1));
        createRoundRecord(joinChallenge(user2));
        roundRecordRepository.flush();

        // when: 두 명의 사용자가 각각 연장 응답(CONTINUE) 처리
        eventListener.handleChallengeExtensionResponseEvent(new ChallengeExtensionResponseEvent(testRound.getId(), user1, NextRoundIntent.CONTINUE));
        eventListener.handleChallengeExtensionResponseEvent(new ChallengeExtensionResponseEvent(testRound.getId(), user2, NextRoundIntent.CONTINUE));

        Thread.sleep(1500);

        // then
        transactionTemplate.executeWithoutResult(status -> {
            // Event는 유니크 제약 조건에 의해 1개만 유지되어야 함
            assertThat(notificationEventRepository.findAll()).hasSize(1);
            // Delivery는 각 유저별로 총 2개여야 함
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