package com.hrr.backend.domain.challenge.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 챌린지 인증 요일 교체(Challenge.updateChallengeDays) 검증 테스트
 */
@DataJpaTest
@DisplayName("챌린지 인증 요일 교체 - Challenge.updateChallengeDays()")
class ChallengeDayJoinReplaceTest {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Autowired
    private ChallengeDayJoinRepository challengeDayJoinRepository;

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

    private Long challengeId;

    @BeforeEach
    void setUp() {
        // Given: 월/수/금으로 챌린지 생성
        Challenge challenge = createChallenge();
        addDays(challenge, ChallengeDays.MONDAY, ChallengeDays.WEDNESDAY, ChallengeDays.FRIDAY);

        challengeRepository.save(challenge);
        flushAndClear();

        challengeId = challenge.getId();
    }

    @Test
    @DisplayName("요일 구성을 바꿔서 수정하면 기존 요일은 모두 삭제되고 새 요일만 남아야 한다")
    void updateChallengeDays_replacesAllDays() {
        // When: 월/수/금 -> 화/목 으로 교체
        Challenge challenge = findWithDays();
        challenge.updateChallengeDays(List.of(ChallengeDays.TUESDAY, ChallengeDays.THURSDAY));
        flushAndClear();

        // Then
        assertThat(findDays()).containsExactlyInAnyOrder(ChallengeDays.TUESDAY, ChallengeDays.THURSDAY);
        assertThat(countAllRows()).isEqualTo(2);
    }

    @Test
    @DisplayName("[회귀] 동일한 요일로 다시 저장해도 요일이 중복 누적되지 않아야 한다")
    void updateChallengeDays_withSameDays_doesNotDuplicate() {
        // When: 월/수/금 -> 월/수/금 (요일은 그대로 두고 다른 항목만 수정한 상황)
        Challenge challenge = findWithDays();
        challenge.updateChallengeDays(List.of(ChallengeDays.MONDAY, ChallengeDays.WEDNESDAY, ChallengeDays.FRIDAY));
        flushAndClear();

        // Then: 월,월,수,수,금,금 (6건)이 아니라 월,수,금 (3건)이어야 한다
        assertThat(findDays()).containsExactlyInAnyOrder(
                ChallengeDays.MONDAY, ChallengeDays.WEDNESDAY, ChallengeDays.FRIDAY);
        assertThat(countAllRows()).isEqualTo(3);
    }

    @Test
    @DisplayName("[회귀] 여러 번 수정해도 요일이 누적되지 않고 마지막 수정 값만 남아야 한다")
    void updateChallengeDays_multipleTimes_doesNotAccumulate() {
        // When: 1차 수정
        findWithDays().updateChallengeDays(List.of(ChallengeDays.MONDAY, ChallengeDays.WEDNESDAY, ChallengeDays.FRIDAY));
        flushAndClear();

        // When: 2차 수정
        findWithDays().updateChallengeDays(List.of(ChallengeDays.TUESDAY));
        flushAndClear();

        // When: 3차 수정
        findWithDays().updateChallengeDays(List.of(ChallengeDays.SATURDAY, ChallengeDays.SUNDAY));
        flushAndClear();

        // Then
        assertThat(findDays()).containsExactlyInAnyOrder(ChallengeDays.SATURDAY, ChallengeDays.SUNDAY);
        assertThat(countAllRows()).isEqualTo(2);
    }

    @Test
    @DisplayName("중복된 요일이 요청으로 들어와도 중복 제거되어 저장되어야 한다")
    void updateChallengeDays_withDuplicatedRequest_appliesDistinct() {
        // When: 요청 자체에 중복 요일이 포함된 경우
        Challenge challenge = findWithDays();
        challenge.updateChallengeDays(List.of(
                ChallengeDays.MONDAY, ChallengeDays.MONDAY, ChallengeDays.MONDAY, ChallengeDays.TUESDAY));
        flushAndClear();

        // Then
        assertThat(findDays()).containsExactlyInAnyOrder(ChallengeDays.MONDAY, ChallengeDays.TUESDAY);
        assertThat(countAllRows()).isEqualTo(2);
    }


    private Challenge findWithDays() {
        return challengeRepository.findByIdWithDays(challengeId).orElseThrow();
    }

    private List<ChallengeDays> findDays() {
        return challengeDayJoinRepository.findByChallengeIdIn(List.of(challengeId)).stream()
                .map(ChallengeDayJoin::getDay)
                .toList();
    }

    // 다른 챌린지가 없는 테스트이므로 전체 행 수 = 해당 챌린지의 요일 행 수
    private long countAllRows() {
        return challengeDayJoinRepository.count();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void addDays(Challenge challenge, ChallengeDays... days) {
        Arrays.stream(days).forEach(day -> challenge.getChallengeDays().add(
                ChallengeDayJoin.builder()
                        .challenge(challenge)
                        .dayOfWeek(day)
                        .build()
        ));
    }

    private Challenge createChallenge() {
        return Challenge.builder()
                .title("요일 교체 테스트")
                .description("테스트 설명")
                .startDate(LocalDateTime.now().plusDays(3))
                .status(ChallengeStatus.UPCOMING)
                .category(Category.values()[0])
                .verificationType(VerificationType.values()[0])
                .isPublic(true)
                .isViewerMode(false)
                .maxParticipants(10)
                .currentParticipants(1)
                .verifyStartTime(LocalTime.of(9, 0))
                .verifyEndTime(LocalTime.of(18, 0))
                .likeCount(0)
                .build();
    }
}