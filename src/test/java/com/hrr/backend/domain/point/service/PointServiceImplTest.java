package com.hrr.backend.domain.point.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.time.Clock;
import java.time.ZoneId;
import org.mockito.Spy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.point.converter.PointConverter;
import com.hrr.backend.domain.point.entity.PointHistory;
import com.hrr.backend.domain.point.entity.enums.PointType;
import com.hrr.backend.domain.point.repository.PointHistoryRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.user.entity.RandomMission;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.repository.VerificationAbsenceLogRepository;
import com.hrr.backend.global.common.enums.ChallengeDays;

@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

    @InjectMocks
    private PointServiceImpl pointService;

    @Mock
    private PointHistoryRepository pointHistoryRepository;
    @Mock
    private RoundRecordRepository roundRecordRepository;
    @Mock
    private VerificationAbsenceLogRepository verificationAbsenceLogRepository;
    @Mock
    private PointConverter pointConverter;
    @Mock
    private PointAwardExecutor pointAwardExecutor;
    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Spy
    private Clock clock = Clock.fixed(
            LocalDate.of(2026, 7, 22).atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    @DisplayName("챌린지 첫 인증 포인트는 아직 지급된 적 없으면 PointAwardExecutor를 호출한다")
    void earnFirstVerificationPoint_awardsPoint_whenNotAlreadyAwarded() {
        // given
        User user = User.builder().id(1L).points(0L).build();
        Challenge challenge = Challenge.builder().id(10L).build();
        Verification verification = Verification.builder().id(500L).build();

        given(pointHistoryRepository.existsByUserAndPointTypeAndChallenge(user, PointType.FIRST_VERIFICATION, challenge))
                .willReturn(false);

        // when
        pointService.earnFirstVerificationPoint(user, challenge, verification);

        // then
        verify(pointAwardExecutor, times(1))
                .execute(user, PointType.FIRST_VERIFICATION, challenge, null, null, verification);
    }

    @Test
    @DisplayName("챌린지 첫 인증 포인트는 이미 지급된 적 있으면 PointAwardExecutor를 호출하지 않는다")
    void earnFirstVerificationPoint_doesNothing_whenAlreadyAwarded() {
        // given
        User user = User.builder().id(1L).points(5L).build();
        Challenge challenge = Challenge.builder().id(10L).build();
        Verification verification = Verification.builder().id(500L).build();

        given(pointHistoryRepository.existsByUserAndPointTypeAndChallenge(user, PointType.FIRST_VERIFICATION, challenge))
                .willReturn(true);

        // when
        pointService.earnFirstVerificationPoint(user, challenge, verification);

        // then
        verify(pointAwardExecutor, never()).execute(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("랜덤미션 참여 포인트는 호출될 때마다 PointAwardExecutor를 호출한다 (일 1회 제한은 호출부 책임)")
    void earnRandomMissionPoint_awardsPoint() {
        // given
        User user = User.builder().id(1L).points(0L).build();
        RandomMission mission = RandomMission.builder().id(100L).title("t").content("c").build();

        // when
        pointService.earnRandomMissionPoint(user, mission);

        // then
        verify(pointAwardExecutor, times(1))
                .execute(user, PointType.RANDOM_MISSION, null, null, mission, null);
    }

    @Test
    @DisplayName("챌린지 마스터 포인트는 참여 라운드 수가 정확히 3이 되는 순간 PointAwardExecutor를 호출한다")
    void checkAndEarnChallengeMasterPoint_awards_whenRoundCountIsExactlyThree() {
        // given
        User user = User.builder().id(1L).points(0L).build();
        Challenge challenge = Challenge.builder().id(10L).build();
        UserChallenge userChallenge = UserChallenge.builder().id(50L).user(user).challenge(challenge).build();

        given(roundRecordRepository.countByUserChallengeId(50L)).willReturn(3L);
        given(pointHistoryRepository.existsByUserAndPointTypeAndChallenge(user, PointType.CHALLENGE_MASTER, challenge))
                .willReturn(false);

        // when
        pointService.checkAndEarnChallengeMasterPoint(user, challenge, userChallenge);

        // then
        verify(pointAwardExecutor, times(1))
                .execute(user, PointType.CHALLENGE_MASTER, challenge, null, null, null);
    }

    @Test
    @DisplayName("챌린지 마스터 포인트는 참여 라운드 수가 3이 아니면 지급되지 않는다")
    void checkAndEarnChallengeMasterPoint_doesNotAward_whenRoundCountNotThree() {
        // given
        User user = User.builder().id(1L).points(0L).build();
        Challenge challenge = Challenge.builder().id(10L).build();
        UserChallenge userChallenge = UserChallenge.builder().id(50L).user(user).challenge(challenge).build();

        given(roundRecordRepository.countByUserChallengeId(50L)).willReturn(4L);

        // when
        pointService.checkAndEarnChallengeMasterPoint(user, challenge, userChallenge);

        // then
        verify(pointAwardExecutor, never()).execute(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("챌린지 마스터 포인트는 이미 지급된 적 있으면 중복 지급되지 않는다")
    void checkAndEarnChallengeMasterPoint_doesNotAward_whenAlreadyAwarded() {
        // given
        User user = User.builder().id(1L).points(0L).build();
        Challenge challenge = Challenge.builder().id(10L).build();
        UserChallenge userChallenge = UserChallenge.builder().id(50L).user(user).challenge(challenge).build();

        given(roundRecordRepository.countByUserChallengeId(50L)).willReturn(3L);
        given(pointHistoryRepository.existsByUserAndPointTypeAndChallenge(user, PointType.CHALLENGE_MASTER, challenge))
                .willReturn(true);

        // when
        pointService.checkAndEarnChallengeMasterPoint(user, challenge, userChallenge);

        // then
        verify(pointAwardExecutor, never()).execute(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("주차 퍼펙트 포인트는 그 주의 마지막 인증일에 결석이 없으면 PointAwardExecutor를 호출한다 (트리거 인증글도 함께 전달)")
    void checkAndEarnWeeklyPerfectPoint_awards_whenLastDayAndNoAbsence() {
        // given: 라운드 시작일은 월요일(2026-07-06), 인증 요일은 월/수/금
        LocalDate roundStart = LocalDate.of(2026, 7, 6); // Monday
        LocalDate roundEnd = roundStart.plusWeeks(3).minusDays(1);

        Challenge challenge = Challenge.builder()
                .id(10L)
                .challengeDays(List.of(
                        ChallengeDayJoin.builder().dayOfWeek(ChallengeDays.MONDAY).build(),
                        ChallengeDayJoin.builder().dayOfWeek(ChallengeDays.WEDNESDAY).build(),
                        ChallengeDayJoin.builder().dayOfWeek(ChallengeDays.FRIDAY).build()
                ))
                .build();

        Round round = Round.builder().id(20L).startDate(roundStart).endDate(roundEnd).build();
        RoundRecord roundRecord = RoundRecord.builder().id(30L).build();
        User user = User.builder().id(1L).points(0L).build();
        UserChallenge userChallenge = UserChallenge.builder().id(50L).user(user).build();
        Verification verification = Verification.builder().id(700L).build();

        // 1주차 마지막 인증일 = 7/10(금)
        LocalDateTime verifiedAt = LocalDateTime.of(2026, 7, 10, 10, 0);

        given(verificationAbsenceLogRepository.countByRoundRecordIdAndAbsenceDateBetween(
                eq(30L), eq(roundStart), eq(roundStart.plusDays(6))
        )).willReturn(0L);
        given(pointHistoryRepository.existsByUserAndPointTypeAndRound(user, PointType.WEEK1_PERFECT, round))
                .willReturn(false);

        // when
        pointService.checkAndEarnWeeklyPerfectPoint(userChallenge, roundRecord, round, challenge, verifiedAt, verification);

        // then
        verify(pointAwardExecutor, times(1))
                .execute(user, PointType.WEEK1_PERFECT, challenge, round, null, verification);
    }

    @Test
    @DisplayName("주차 퍼펙트 포인트는 마지막 인증일이 아니면 판단하지 않고 지급하지 않는다")
    void checkAndEarnWeeklyPerfectPoint_doesNotAward_whenNotLastVerificationDay() {
        // given
        LocalDate roundStart = LocalDate.of(2026, 7, 6);
        LocalDate roundEnd = roundStart.plusWeeks(3).minusDays(1);

        Challenge challenge = Challenge.builder()
                .id(10L)
                .challengeDays(List.of(
                        ChallengeDayJoin.builder().dayOfWeek(ChallengeDays.MONDAY).build(),
                        ChallengeDayJoin.builder().dayOfWeek(ChallengeDays.WEDNESDAY).build(),
                        ChallengeDayJoin.builder().dayOfWeek(ChallengeDays.FRIDAY).build()
                ))
                .build();

        Round round = Round.builder().id(20L).startDate(roundStart).endDate(roundEnd).build();
        RoundRecord roundRecord = RoundRecord.builder().id(30L).build();
        User user = User.builder().id(1L).points(0L).build();
        UserChallenge userChallenge = UserChallenge.builder().id(50L).user(user).build();
        Verification verification = Verification.builder().id(700L).build();

        // 1주차 인증일이지만 마지막 날(금)이 아닌 수요일(7/8)에 인증
        LocalDateTime verifiedAt = LocalDateTime.of(2026, 7, 8, 10, 0);

        // when
        pointService.checkAndEarnWeeklyPerfectPoint(userChallenge, roundRecord, round, challenge, verifiedAt, verification);

        // then: 마지막 날이 아니므로 결석 조회조차 하지 않고 즉시 반환
        verify(verificationAbsenceLogRepository, never())
                .countByRoundRecordIdAndAbsenceDateBetween(any(), any(), any());
        verify(pointAwardExecutor, never()).execute(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("주차 퍼펙트 포인트는 그 주에 결석 기록이 있으면 지급되지 않는다")
    void checkAndEarnWeeklyPerfectPoint_doesNotAward_whenAbsenceExistsInWeek() {
        // given
        LocalDate roundStart = LocalDate.of(2026, 7, 6);
        LocalDate roundEnd = roundStart.plusWeeks(3).minusDays(1);

        Challenge challenge = Challenge.builder()
                .id(10L)
                .challengeDays(List.of(
                        ChallengeDayJoin.builder().dayOfWeek(ChallengeDays.MONDAY).build(),
                        ChallengeDayJoin.builder().dayOfWeek(ChallengeDays.WEDNESDAY).build(),
                        ChallengeDayJoin.builder().dayOfWeek(ChallengeDays.FRIDAY).build()
                ))
                .build();

        Round round = Round.builder().id(20L).startDate(roundStart).endDate(roundEnd).build();
        RoundRecord roundRecord = RoundRecord.builder().id(30L).build();
        User user = User.builder().id(1L).points(0L).build();
        UserChallenge userChallenge = UserChallenge.builder().id(50L).user(user).build();
        Verification verification = Verification.builder().id(700L).build();

        LocalDateTime verifiedAt = LocalDateTime.of(2026, 7, 10, 10, 0); // 1주차 마지막 인증일(금)

        given(verificationAbsenceLogRepository.countByRoundRecordIdAndAbsenceDateBetween(
                eq(30L), eq(roundStart), eq(roundStart.plusDays(6))
        )).willReturn(1L); // 결석 1회 존재

        // when
        pointService.checkAndEarnWeeklyPerfectPoint(userChallenge, roundRecord, round, challenge, verifiedAt, verification);

        // then
        verify(pointAwardExecutor, never()).execute(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("무결석 완주 포인트는 결석이 없는 참여자에게만 지급되고, 이미 지급된 참여자는 건너뛴다")
    void checkAndEarnFlawlessRoundPoints_awardsOnlyForZeroAbsenceAndNotAlreadyAwarded() {
        // given
        Challenge challenge = Challenge.builder().id(10L).build();
        Round endedRound = Round.builder().id(20L).build();

        User noAbsenceUser = User.builder().id(1L).points(0L).build();
        UserChallenge ucNoAbsence = UserChallenge.builder().id(51L).user(noAbsenceUser).challenge(challenge).build();
        RoundRecord rrNoAbsence = RoundRecord.builder().id(31L).userChallenge(ucNoAbsence).build();

        User hasAbsenceUser = User.builder().id(2L).points(0L).build();
        UserChallenge ucHasAbsence = UserChallenge.builder().id(52L).user(hasAbsenceUser).challenge(challenge).build();
        RoundRecord rrHasAbsence = RoundRecord.builder().id(32L).userChallenge(ucHasAbsence).build();

        User alreadyAwardedUser = User.builder().id(3L).points(0L).build();
        UserChallenge ucAlreadyAwarded = UserChallenge.builder().id(53L).user(alreadyAwardedUser).challenge(challenge).build();
        RoundRecord rrAlreadyAwarded = RoundRecord.builder().id(33L).userChallenge(ucAlreadyAwarded).build();

        given(roundRecordRepository.findAllByRoundIdWithUserAndChallenge(20L))
                .willReturn(List.of(rrNoAbsence, rrHasAbsence, rrAlreadyAwarded));

        given(verificationAbsenceLogRepository.countByRoundRecordId(31L)).willReturn(0L);
        given(verificationAbsenceLogRepository.countByRoundRecordId(32L)).willReturn(1L);
        given(verificationAbsenceLogRepository.countByRoundRecordId(33L)).willReturn(0L);

        given(pointHistoryRepository.existsByUserAndPointTypeAndRound(noAbsenceUser, PointType.FLAWLESS_ROUND, endedRound))
                .willReturn(false);
        given(pointHistoryRepository.existsByUserAndPointTypeAndRound(alreadyAwardedUser, PointType.FLAWLESS_ROUND, endedRound))
                .willReturn(true);

        // when
        pointService.checkAndEarnFlawlessRoundPoints(endedRound);

        // then: 결석 없고 미지급 상태인 noAbsenceUser에 대해서만 호출 (verification은 항상 null)
        verify(pointAwardExecutor, times(1))
                .execute(noAbsenceUser, PointType.FLAWLESS_ROUND, challenge, endedRound, null, null);
        verify(pointAwardExecutor, never())
                .execute(eq(hasAbsenceUser), any(), any(), any(), any(), any());
        verify(pointAwardExecutor, never())
                .execute(eq(alreadyAwardedUser), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("동시 요청 등으로 PointAwardExecutor가 유니크 제약 위반 예외를 던져도, 상위 로직은 예외 없이 정상 종료된다")
    void earnFirstVerificationPoint_silentlyIgnoresDataIntegrityViolation() {
        // given
        User user = User.builder().id(1L).points(0L).build();
        Challenge challenge = Challenge.builder().id(10L).build();
        Verification verification = Verification.builder().id(700L).build();

        given(pointHistoryRepository.existsByUserAndPointTypeAndChallenge(user, PointType.FIRST_VERIFICATION, challenge))
                .willReturn(false);
        willThrow(new DataIntegrityViolationException("duplicate"))
                .given(pointAwardExecutor).execute(user, PointType.FIRST_VERIFICATION, challenge, null, null, verification);

        // when & then: 예외가 밖으로 전파되지 않아야 함 (동시성으로 인한 중복은 정상 케이스로 흡수)
        assertThatCode(() -> pointService.earnFirstVerificationPoint(user, challenge, verification))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("revokePointsForVerification: 그 인증글로 지급된 포인트가 있으면 전부 삭제하고 유저 포인트를 합계만큼 원자적으로 차감한다")
    void revokePointsForVerification_deletesHistoriesAndDecreasesUserPoints() {
        // given
        User owner = User.builder().id(1L).points(10L).build();
        Challenge challenge = Challenge.builder().id(10L).build();
        UserChallenge userChallenge = UserChallenge.builder().id(50L).user(owner).challenge(challenge).build();
        RoundRecord roundRecord = RoundRecord.builder().id(30L).userChallenge(userChallenge).build();
        Verification verification = Verification.builder().id(999L).roundRecord(roundRecord).build();

        PointHistory firstVerificationHistory = PointHistory.builder()
                .id(1L).points(1).pointType(PointType.FIRST_VERIFICATION).build();
        PointHistory weekPerfectHistory = PointHistory.builder()
                .id(2L).points(3).pointType(PointType.WEEK1_PERFECT).build();

        given(pointHistoryRepository.findAllByVerificationIdForUpdate(999L))
                .willReturn(List.of(firstVerificationHistory, weekPerfectHistory));
        // when
        pointService.revokePointsForVerification(verification);

        // then: 1P + 3P = 4P 회수
        verify(pointHistoryRepository, times(1))
                .deleteAll(List.of(firstVerificationHistory, weekPerfectHistory));
        verify(userRepository, times(1)).decreasePoints(1L, 4L);
    }

    @Test
    @DisplayName("revokePointsForVerification: 그 인증글로 지급된 포인트가 없으면 아무 것도 하지 않는다")
    void revokePointsForVerification_doesNothing_whenNoLinkedHistory() {
        // given
        User owner = User.builder().id(1L).points(10L).build();
        UserChallenge userChallenge = UserChallenge.builder().id(50L).user(owner).build();
        RoundRecord roundRecord = RoundRecord.builder().id(30L).userChallenge(userChallenge).build();
        Verification verification = Verification.builder().id(999L).roundRecord(roundRecord).build();

        given(pointHistoryRepository.findAllByVerificationIdForUpdate(999L)).willReturn(List.of());

        // when
        pointService.revokePointsForVerification(verification);

        // then
        verify(pointHistoryRepository, never()).deleteAll(any());
        verify(userRepository, never()).decreasePoints(any(), anyLong());
    }
}