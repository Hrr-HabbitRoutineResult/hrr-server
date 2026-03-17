package com.hrr.backend.domain.verification;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 미인증 집계 로직 TDD 테스트
 * - 전제: 챌린지 상태는 ONGOING이지만, 라운드 시작일이 오늘인 경우
 * - 기대: 어제 날짜는 라운드 기간 밖이므로 미인증 집계 대상이 되면 안 된다
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 실제 설정(h2) 사용
class VerificationAbsenceRepositoryTest {

    // 리포지토리 주입
	@Autowired
	private RoundRecordRepository roundRecordRepository;
	@Autowired
	private RoundRepository roundRepository;
	@Autowired
	private ChallengeRepository challengeRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private UserChallengeRepository userChallengeRepository;

    @Test
    @DisplayName("라운드 시작 전 날짜는 미인증 집계 대상이 아니다")
    void shouldNotCountAbsence_WhenDateIsBeforeRoundStart() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // 어제 요일을 챌린지 인증 요일에 포함시킨다
        ChallengeDays yesterdayChallengeDay = ChallengeDays.from(yesterday.getDayOfWeek());

        // 유저 생성 (필수값은 빌더 기본값 사용)
        User user = userRepository.save(User.builder()
                .name("tester")
                .nickname("tester_nick")
                .isPublic(true)
                .build());

        // 챌린지 생성: 상태 ONGOING, 시작일은 '오늘' 00:00
        Challenge challenge = challengeRepository.save(Challenge.builder()
                .isPublic(true)
                .category(Category.HEALTH)
                .isViewerMode(false)
                .maxParticipants(10)
                .title("TDD Challenge")
                .description("absence counting test")
                .startDate(LocalDateTime.of(today, LocalTime.MIDNIGHT))
                .verificationType(VerificationType.TEXT)
                .verifyStartTime(LocalTime.of(9, 0))
                .verifyEndTime(LocalTime.of(22, 0))
                .currentParticipants(1)
                .status(ChallengeStatus.ONGOING)
                .build());

        // 인증 요일 추가 (어제 요일)
        ChallengeDayJoin dayJoin = ChallengeDayJoin.builder()
                .challenge(challenge)
                .dayOfWeek(yesterdayChallengeDay)
                .build();
        challenge.getChallengeDays().add(dayJoin);
        challenge = challengeRepository.save(challenge);

        // 라운드 생성: 시작일은 오늘, 종료일은 3주 뒤
        Round round = roundRepository.save(Round.builder()
                .challenge(challenge)
                .roundNumber(1)
                .startDate(today)
                .endDate(today.plusWeeks(3))
                .build());

        // 챌린지의 currentRound를 설정
        challenge.changeCurrentRound(round);
        challenge = challengeRepository.save(challenge);

        // 유저의 챌린지 참여 (JOINED)
        UserChallenge uc = userChallengeRepository.save(UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .build());

        // 라운드 레코드 생성
        roundRecordRepository.save(RoundRecord.builder()
                .round(round)
                .userChallenge(uc)
                .build());

        // when: 어제 날짜로 미인증 대상 조회
        List<RoundRecord> absentees = roundRecordRepository.findAbsentees(yesterdayChallengeDay, yesterday);

        // then: 라운드 시작 전 날짜이므로 집계되면 안 된다
        // 현재 구현에서는 날짜 범위 검증이 빠져 있어 실패가 예상됨 (TDD용 실패 테스트)
        assertThat(absentees).isEmpty();
    }
}

