package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.dto.ChallengeRequestDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeStaticsRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import jakarta.persistence.EntityManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChallengeJoinConcurrencyIntegrationTest {

    private static final int MAX_PARTICIPANTS = 30;
    private static final int INITIAL_PARTICIPANTS = 29;
    private static final int CONCURRENT_REQUESTS = 50;
    private static final AtomicInteger USER_SEQUENCE = new AtomicInteger();

    @Autowired private ChallengeService challengeService;
    @Autowired private ChallengeRepository challengeRepository;
    @Autowired private ChallengeStaticsRepository challengeStaticsRepository;
    @Autowired private RoundRepository roundRepository;
    @Autowired private RoundRecordRepository roundRecordRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserChallengeRepository userChallengeRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    @DisplayName("joinChallenge 동시 호출 시 최종 참가자 수가 정원을 초과하지 않는다")
    void joinChallenge_WithPessimisticLock_DoesNotAllowOverCapacity() throws InterruptedException {
        Long challengeId = createChallengeWithCurrentRoundAndParticipants();
        List<Long> userIds = createUsers("joiner", CONCURRENT_REQUESTS);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(CONCURRENT_REQUESTS);
        ExecutorService executorService = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);

        for (Long userId : userIds) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    User user = userRepository.findById(userId).orElseThrow();
                    ChallengeRequestDto.JoinChallengeDto request = new ChallengeRequestDto.JoinChallengeDto();

                    challengeService.joinChallenge(user, challengeId, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        boolean completed = endLatch.await(20, TimeUnit.SECONDS);
        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(completed).as("모든 joinChallenge 요청이 제한 시간 안에 완료되어야 한다").isTrue();
        assertThat(terminated).as("ExecutorService가 정상 종료되어야 한다").isTrue();

        entityManager.clear();

        Challenge challenge = challengeRepository.findById(challengeId).orElseThrow();
        long joinedCount = userChallengeRepository.countByChallengeIdAndStatus(
                challengeId,
                ChallengeJoinStatus.JOINED
        );

        printResult(challenge.getCurrentParticipants(), joinedCount, successCount.get(), failureCount.get());

        assertThat(successCount.get() + failureCount.get())
                .as("성공/실패 요청 수 합계")
                .isEqualTo(CONCURRENT_REQUESTS);
        assertThat(challenge.getCurrentParticipants())
                .as("Challenge.currentParticipants는 정원까지만 증가해야 한다")
                .isEqualTo(MAX_PARTICIPANTS);
        assertThat(joinedCount)
                .as("JOINED 상태 UserChallenge 개수는 정원과 같아야 한다")
                .isEqualTo(MAX_PARTICIPANTS);
        assertThat(joinedCount)
                .as("JOINED 상태 UserChallenge 개수는 maxParticipants를 초과하면 안 된다")
                .isLessThanOrEqualTo(challenge.getMaxParticipants());
    }

    private Long createChallengeWithCurrentRoundAndParticipants() {
        return transactionTemplate.execute(status -> {
            LocalDate startDate = LocalDate.now();

            Challenge challenge = challengeRepository.save(Challenge.builder()
                    .title("concurrency")
                    .description("join race test")
                    .isPublic(true)
                    .isViewerMode(false)
                    .category(Category.HEALTH)
                    .verificationType(VerificationType.TEXT)
                    .startDate(LocalDateTime.of(startDate, LocalTime.MIDNIGHT))
                    .verifyStartTime(LocalTime.of(9, 0))
                    .verifyEndTime(LocalTime.of(22, 0))
                    .maxParticipants(MAX_PARTICIPANTS)
                    .currentParticipants(INITIAL_PARTICIPANTS)
                    .status(ChallengeStatus.RECRUITING)
                    .imageKey("test-image-key")
                    .build());

            Round currentRound = roundRepository.save(Round.builder()
                    .challenge(challenge)
                    .roundNumber(1)
                    .startDate(startDate)
                    .endDate(startDate.plusWeeks(Challenge.ROUND_WEEKS).minusDays(1))
                    .build());

            challenge.changeCurrentRound(currentRound);

            List<User> existingUsers = createUsersInCurrentTransaction("existing", INITIAL_PARTICIPANTS);
            for (User user : existingUsers) {
                UserChallenge userChallenge = userChallengeRepository.save(UserChallenge.builder()
                        .user(user)
                        .challenge(challenge)
                        .status(ChallengeJoinStatus.JOINED)
                        .build());

                roundRecordRepository.save(RoundRecord.builder()
                        .round(currentRound)
                        .userChallenge(userChallenge)
                        .build());
            }

            return challenge.getId();
        });
    }

    private List<Long> createUsers(String prefix, int count) {
        return transactionTemplate.execute(status ->
                createUsersInCurrentTransaction(prefix, count).stream()
                        .map(User::getId)
                        .toList()
        );
    }

    private List<User> createUsersInCurrentTransaction(String prefix, int count) {
        List<User> users = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String uniqueName = prefix + "_" + USER_SEQUENCE.incrementAndGet();
            users.add(userRepository.save(User.builder()
                    .name(uniqueName)
                    .nickname(uniqueName)
                    .isPublic(true)
                    .build()));
        }

        return users;
    }

    private void printResult(Integer currentParticipants, long joinedCount, int successCount, int failureCount) {
        System.out.println();
        System.out.println("========== joinChallenge concurrency test result ==========");
        System.out.printf("Challenge.currentParticipants : %d%n", currentParticipants);
        System.out.printf("JOINED UserChallenge count    : %d%n", joinedCount);
        System.out.printf("success request count         : %d%n", successCount);
        System.out.printf("failure request count         : %d%n", failureCount);
        System.out.println("===========================================================");
        System.out.println();
    }

    private void cleanDatabase() {
        transactionTemplate.executeWithoutResult(status -> {
            challengeStaticsRepository.deleteAll();
            roundRecordRepository.deleteAll();
            roundRepository.deleteAll();
            userChallengeRepository.deleteAll();
            challengeRepository.deleteAll();
            userRepository.deleteAll();
        });
    }
}
