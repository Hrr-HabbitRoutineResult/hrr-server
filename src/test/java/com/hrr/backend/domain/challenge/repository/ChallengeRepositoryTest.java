package com.hrr.backend.domain.challenge.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ChallengeRepositoryTest {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private TestEntityManager entityManager;

    @TestConfiguration
    static class QueryDslTestConfig {
        @PersistenceContext
        private EntityManager entityManager;

        @Bean
        public JPAQueryFactory jpaQueryFactory() {
            return new JPAQueryFactory(entityManager);
        }
    }

    @Test
    @DisplayName("Bulk Update: 기준 시각(오늘 0시) 이전(포함)의 UPCOMING 챌린지들은 ONGOING으로 상태가 일괄 변경되어야 한다")
    void updateChallengeStatusToOngoing_UpdatesCorrectChallenges() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        // [Target] 오늘 0시에 시작하는 챌린지 (UPCOMING -> ONGOING 대상)
        Challenge targetToday = createChallenge("오늘시작", todayStart, ChallengeStatus.UPCOMING);

        // [Target] 어제 0시에 시작했어야 하는 챌린지 (UPCOMING -> ONGOING 대상)
        Challenge targetPast = createChallenge("과거시작", yesterdayStart, ChallengeStatus.UPCOMING);

        // [Non-Target] 내일 0시에 시작하는 챌린지 (변경되면 안 됨)
        Challenge futureChallenge = createChallenge("미래시작", tomorrowStart, ChallengeStatus.UPCOMING);

        // [Non-Target] 이미 종료된 챌린지 (변경되면 안 됨)
        Challenge finishedChallenge = createChallenge("이미종료", todayStart, ChallengeStatus.FINISHED);

        // DB 반영
        entityManager.persist(targetToday);
        entityManager.persist(targetPast);
        entityManager.persist(futureChallenge);
        entityManager.persist(finishedChallenge);
        entityManager.flush();
        entityManager.clear();

        // When
        LocalDateTime referenceTime = todayStart;

        int updatedCount = challengeRepository.updateChallengeStatusToOngoing(
                ChallengeStatus.ONGOING,
                ChallengeStatus.UPCOMING,
                referenceTime
        );

        // Then
        // 업데이트된 개수 검증 (Today + Past = 2개)
        assertThat(updatedCount).isEqualTo(2);

        // 상태 변경 검증
        Challenge updatedToday = challengeRepository.findById(targetToday.getId()).orElseThrow();
        Challenge updatedPast = challengeRepository.findById(targetPast.getId()).orElseThrow();
        Challenge ignoredFuture = challengeRepository.findById(futureChallenge.getId()).orElseThrow();
        Challenge ignoredFinished = challengeRepository.findById(finishedChallenge.getId()).orElseThrow();

        assertThat(updatedToday.getStatus()).isEqualTo(ChallengeStatus.ONGOING);
        assertThat(updatedPast.getStatus()).isEqualTo(ChallengeStatus.ONGOING);

        assertThat(ignoredFuture.getStatus()).isEqualTo(ChallengeStatus.UPCOMING); // 그대로여야 함
        assertThat(ignoredFinished.getStatus()).isEqualTo(ChallengeStatus.FINISHED); // 그대로여야 함
    }

    @Test
    @DisplayName("findIdsToStart: 기준 시각(오늘 0시) 이전(포함)의 UPCOMING 챌린지 ID만 조회해야 한다")
    void findIdsToStart_ReturnsCorrectIds() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = today.minusDays(1).atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        Challenge targetToday = createChallenge("오늘시작", todayStart, ChallengeStatus.UPCOMING);
        Challenge targetPast = createChallenge("과거시작", yesterdayStart, ChallengeStatus.UPCOMING);
        Challenge futureChallenge = createChallenge("미래시작", tomorrowStart, ChallengeStatus.UPCOMING);
        Challenge finishedChallenge = createChallenge("이미종료", todayStart, ChallengeStatus.FINISHED);

        entityManager.persist(targetToday);
        entityManager.persist(targetPast);
        entityManager.persist(futureChallenge);
        entityManager.persist(finishedChallenge);
        entityManager.flush();
        entityManager.clear();

        // When
        List<Long> ids = challengeRepository.findIdsToStart(
                ChallengeStatus.UPCOMING,
                todayStart
        );

        // Then
        assertThat(ids)
                .containsExactlyInAnyOrder(targetToday.getId(), targetPast.getId())
                .doesNotContain(futureChallenge.getId(), finishedChallenge.getId());
    }

    // 테스트용 챌린지 생성 헬퍼
    private Challenge createChallenge(String title, LocalDateTime startDate, ChallengeStatus status) {
        return Challenge.builder()
                .title(title)
                .description("테스트 설명")
                .startDate(startDate)
                .status(status)
                .category(Category.values()[0])
                .verificationType(VerificationType.values()[0])
                .isPublic(true)
                .isViewerMode(false)
                .maxParticipants(10)
                .currentParticipants(0)
                .verifyStartTime(LocalTime.of(9, 0))
                .verifyEndTime(LocalTime.of(18, 0))
                .build();
    }
}
