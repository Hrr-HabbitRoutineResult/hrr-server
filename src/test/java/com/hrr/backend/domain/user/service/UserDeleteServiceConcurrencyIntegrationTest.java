package com.hrr.backend.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrr.backend.domain.auth.service.AuthService;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.follow.service.FollowCountService;
import com.hrr.backend.domain.notification.event.ChallengeVacancyEvent;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import com.hrr.backend.global.s3.S3Service;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@RecordApplicationEvents
class UserDeleteServiceConcurrencyIntegrationTest {

	private static final AtomicInteger USER_SEQUENCE = new AtomicInteger();

	@Autowired private UserDeleteService userDeleteService;
	@Autowired private ChallengeRepository challengeRepository;
	@Autowired private UserChallengeRepository userChallengeRepository;
	@Autowired private UserRepository userRepository;
	@Autowired private FollowRepository followRepository;
	@Autowired private TransactionTemplate transactionTemplate;
	@Autowired private EntityManager entityManager;
	@Autowired private ApplicationEvents applicationEvents;

	@MockitoBean private AuthService authService;
	@MockitoBean private S3Service s3Service;
	@MockitoBean private FollowCountService followCountService;

	@BeforeEach
	void setUp() {
		cleanDatabase();
	}

	@AfterEach
	void tearDown() {
		cleanDatabase();
	}

	@Test
	@DisplayName("동시에 두 명이 탈퇴해도 만석에서 빈자리 이벤트는 한 번만 발행된다")
	void processPermanentWithdrawal_WhenTwoUsersLeaveFullChallenge_PublishesVacancyEventOnce()
			throws InterruptedException {
		WithdrawalTarget target = createFullChallengeWithTwoUsers();

		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch endLatch = new CountDownLatch(target.userIds().size());
		ExecutorService executorService = Executors.newFixedThreadPool(target.userIds().size());
		List<Exception> unexpectedErrors = Collections.synchronizedList(new ArrayList<>());

		for (Long userId : target.userIds()) {
			executorService.submit(() -> {
				try {
					startLatch.await();

					userDeleteService.processPermanentWithdrawal(userId);
				} catch (Exception e) {
					unexpectedErrors.add(e);
				} finally {
					endLatch.countDown();
				}
			});
		}

		startLatch.countDown();

		boolean completed = endLatch.await(20, TimeUnit.SECONDS);
		executorService.shutdown();
		boolean terminated = executorService.awaitTermination(10, TimeUnit.SECONDS);

		if (!terminated) {
			executorService.shutdownNow();
		}

		assertThat(completed).as("모든 탈퇴 요청이 제한 시간 안에 완료되어야 한다").isTrue();
		assertThat(terminated).as("ExecutorService가 정상 종료되어야 한다").isTrue();
		assertThat(unexpectedErrors).as("예상하지 못한 예외가 없어야 한다").isEmpty();

		entityManager.clear();

		Challenge challenge = challengeRepository.findById(target.challengeId()).orElseThrow();
		long kickedCount = userChallengeRepository.countByChallengeIdAndStatus(
				target.challengeId(),
				ChallengeJoinStatus.KICKED
		);

		assertThat(challenge.getCurrentParticipants()).isZero();
		assertThat(kickedCount).isEqualTo(2);
		assertThat(applicationEvents.stream(ChallengeVacancyEvent.class)).hasSize(1);
	}

	@Test
	@Transactional
	@DisplayName("탈퇴 대상 챌린지는 챌린지 ID 오름차순으로 조회되어 락 획득 순서가 고정된다")
	void findByUserAndStatus_ReturnsJoinedChallengesOrderedByChallengeId() {
		OrderedWithdrawalTarget target = createUserJoinedToTwoChallengesInReverseMappingOrder();

		entityManager.clear();

		User user = userRepository.findById(target.userIds().get(0)).orElseThrow();

		List<Long> challengeIds = userChallengeRepository.findByUserAndStatus(user, ChallengeJoinStatus.JOINED)
				.stream()
				.map(userChallenge -> userChallenge.getChallenge().getId())
				.toList();

		assertThat(challengeIds).containsExactlyElementsOf(
				target.challengeIds().stream()
						.sorted()
						.toList()
		);
	}

	private WithdrawalTarget createFullChallengeWithTwoUsers() {
		return transactionTemplate.execute(status -> {
			Challenge challenge = challengeRepository.save(Challenge.builder()
					.title("withdrawal concurrency")
					.description("vacancy event race test")
					.isPublic(true)
					.isViewerMode(false)
					.category(Category.HEALTH)
					.verificationType(VerificationType.TEXT)
					.startDate(LocalDateTime.now().plusDays(1))
					.verifyStartTime(LocalTime.of(9, 0))
					.verifyEndTime(LocalTime.of(22, 0))
					.maxParticipants(2)
					.currentParticipants(2)
					.status(ChallengeStatus.RECRUITING)
					.imageKey("test-image-key")
					.build());

			List<Long> userIds = new ArrayList<>();
			for (int i = 0; i < 2; i++) {
				String uniqueName = "withdrawer_" + USER_SEQUENCE.incrementAndGet();
				User user = userRepository.save(User.builder()
						.name(uniqueName)
						.nickname(uniqueName)
						.isPublic(true)
						.build());

				userChallengeRepository.save(UserChallenge.builder()
						.user(user)
						.challenge(challenge)
						.status(ChallengeJoinStatus.JOINED)
						.build());

				userIds.add(user.getId());
			}

			return new WithdrawalTarget(challenge.getId(), userIds);
		});
	}

	private OrderedWithdrawalTarget createUserJoinedToTwoChallengesInReverseMappingOrder() {
		return transactionTemplate.execute(status -> {
			Challenge firstChallenge = challengeRepository.save(createChallenge("first challenge", 1));
			Challenge secondChallenge = challengeRepository.save(createChallenge("second challenge", 2));

			User user = userRepository.save(User.builder()
					.name("ordered_withdrawer_" + USER_SEQUENCE.incrementAndGet())
					.nickname("ordered_withdrawer_" + USER_SEQUENCE.incrementAndGet())
					.isPublic(true)
					.build());

			userChallengeRepository.save(UserChallenge.builder()
					.user(user)
					.challenge(secondChallenge)
					.status(ChallengeJoinStatus.JOINED)
					.build());
			userChallengeRepository.save(UserChallenge.builder()
					.user(user)
					.challenge(firstChallenge)
					.status(ChallengeJoinStatus.JOINED)
					.build());

			return new OrderedWithdrawalTarget(
					List.of(user.getId()),
					List.of(firstChallenge.getId(), secondChallenge.getId())
			);
		});
	}

	private Challenge createChallenge(String title, int participantCount) {
		return Challenge.builder()
				.title(title)
				.description("ordered lock test")
				.isPublic(true)
				.isViewerMode(false)
				.category(Category.HEALTH)
				.verificationType(VerificationType.TEXT)
				.startDate(LocalDateTime.now().plusDays(1))
				.verifyStartTime(LocalTime.of(9, 0))
				.verifyEndTime(LocalTime.of(22, 0))
				.maxParticipants(2)
				.currentParticipants(participantCount)
				.status(ChallengeStatus.RECRUITING)
				.imageKey("test-image-key")
				.build();
	}

	private void cleanDatabase() {
		transactionTemplate.executeWithoutResult(status -> {
			followRepository.deleteAll();
			userChallengeRepository.deleteAll();
			challengeRepository.deleteAll();
			userRepository.deleteAll();
		});
	}

	private record WithdrawalTarget(Long challengeId, List<Long> userIds) {
	}

	private record OrderedWithdrawalTarget(List<Long> userIds, List<Long> challengeIds) {
	}
}
