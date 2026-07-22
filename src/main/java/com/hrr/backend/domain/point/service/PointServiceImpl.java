package com.hrr.backend.domain.point.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.point.converter.PointConverter;
import com.hrr.backend.domain.point.dto.PointHistoryResponseDto;
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
import com.hrr.backend.global.response.SliceResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImpl implements PointService {

    // 포인트 내역 조회 시 최대 몇 개월치까지 노출할지 (당월 포함 3개월)
    private static final int RECENT_MONTHS = 3;

    // 챌린지 라운드 길이(주). Challenge.ROUND_WEEKS와 동일한 값을 사용
    private static final int ROUND_WEEKS = Challenge.ROUND_WEEKS;

    private final PointHistoryRepository pointHistoryRepository;
    private final RoundRecordRepository roundRecordRepository;
    private final VerificationAbsenceLogRepository verificationAbsenceLogRepository;
    private final PointConverter pointConverter;
    private final PointAwardExecutor pointAwardExecutor;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void earnFirstVerificationPoint(User user, Challenge challenge, Verification verification) {
        if (pointHistoryRepository.existsByUserAndPointTypeAndChallenge(user, PointType.FIRST_VERIFICATION, challenge)) {
            return;
        }
        awardPoint(user, PointType.FIRST_VERIFICATION, challenge, null, null, verification);
    }

    @Override
    @Transactional
    public void earnRandomMissionPoint(User user, RandomMission randomMission) {
        awardPoint(user, PointType.RANDOM_MISSION, null, null, randomMission, null);
    }

    @Override
    @Transactional
    public void checkAndEarnFlawlessRoundPoints(Round endedRound) {
        // 종료된 라운드에 속한 모든 RoundRecord 조회 (JOINED/DROPPED/KICKED 무관)
        List<RoundRecord> records = roundRecordRepository.findAllByRoundIdWithUserAndChallenge(endedRound.getId());

        for (RoundRecord record : records) {
            try {
                long absenceCount = verificationAbsenceLogRepository.countByRoundRecordId(record.getId());
                if (absenceCount > 0) {
                    continue; // 결석이 있으면 무결석 완주가 아님
                }

                User user = record.getUserChallenge().getUser();
                Challenge challenge = record.getUserChallenge().getChallenge();

                if (pointHistoryRepository.existsByUserAndPointTypeAndRound(user, PointType.FLAWLESS_ROUND, endedRound)) {
                    continue; // 이미 지급됨 (재처리 대비 방어)
                }

                awardPoint(user, PointType.FLAWLESS_ROUND, challenge, endedRound, null, null);
            } catch (Exception e) {
                log.error("[Point] 무결석 완주 포인트 지급 실패. roundRecordId={}", record.getId(), e);
            }
        }
    }

    @Override
    @Transactional
    public void checkAndEarnChallengeMasterPoint(User user, Challenge challenge, UserChallenge userChallenge) {
        long roundCount = roundRecordRepository.countByUserChallengeId(userChallenge.getId());

        // 정확히 3라운드째가 되는 순간에만 지급 (그 이후 라운드는 지급 대상 아님)
        if (roundCount != 3) {
            return;
        }

        if (pointHistoryRepository.existsByUserAndPointTypeAndChallenge(user, PointType.CHALLENGE_MASTER, challenge)) {
            return;
        }

        awardPoint(user, PointType.CHALLENGE_MASTER, challenge, null, null, null);
    }

    @Override
    @Transactional
    public void checkAndEarnWeeklyPerfectPoint(
            UserChallenge userChallenge,
            RoundRecord roundRecord,
            Round round,
            Challenge challenge,
            LocalDateTime verifiedAt,
            Verification verification
    ) {
        LocalDate verifiedDate = verifiedAt.toLocalDate();
        LocalDate roundStart = round.getStartDate();

        long daysFromStart = ChronoUnit.DAYS.between(roundStart, verifiedDate);
        if (daysFromStart < 0) {
            return;
        }

        int weekIndex = (int) (daysFromStart / 7); // 0: 1주차, 1: 2주차, 2: 3주차
        if (weekIndex < 0 || weekIndex >= ROUND_WEEKS) {
            return; // 라운드 범위를 벗어나면 무시 (방어 코드)
        }

        LocalDate weekStart = roundStart.plusDays(weekIndex * 7L);
        LocalDate weekEnd = weekStart.plusDays(6);
        if (weekEnd.isAfter(round.getEndDate())) {
            weekEnd = round.getEndDate();
        }

        // 이번 주의 "마지막 인증 요일"을 계산
        LocalDate lastVerificationDayOfWeek = findLastVerificationDay(challenge, weekStart, weekEnd);

        // 오늘이 이번 주의 마지막 인증일이 아니면 아직 판단하지 않음(추후 마지막 날 인증 시 판단)
        if (lastVerificationDayOfWeek == null || !lastVerificationDayOfWeek.equals(verifiedDate)) {
            return;
        }

        // 이번 주 범위 내 결석 로그가 있는지 확인 (전날 00:05 배치가 이미 확정해둔 데이터)
        long absenceCount = verificationAbsenceLogRepository
                .countByRoundRecordIdAndAbsenceDateBetween(roundRecord.getId(), weekStart, weekEnd);
        if (absenceCount > 0) {
            return;
        }

        PointType weekType = resolveWeekPointType(weekIndex);

        if (pointHistoryRepository.existsByUserAndPointTypeAndRound(userChallenge.getUser(), weekType, round)) {
            return; // 이미 지급됨 (재처리 대비 방어)
        }

        awardPoint(userChallenge.getUser(), weekType, challenge, round, null, verification);
    }

    @Override
    @Transactional
    public void revokePointsForVerification(Verification verification) {
        List<PointHistory> histories = pointHistoryRepository.findAllByVerificationId(verification.getId());

        if (histories.isEmpty()) {
            return; // 이 인증글로 지급된 포인트가 없으면 할 일 없음
        }

        long totalToRevoke = histories.stream()
                .mapToLong(PointHistory::getPoints)
                .sum();

        User user = verification.getRoundRecord().getUserChallenge().getUser();

        // 인증 삭제와 같은 트랜잭션 안에서 호출되어, 인증 삭제와 포인트 회수가 함께 성공/실패한다.
        pointHistoryRepository.deleteAll(histories);
        userRepository.decreasePoints(user.getId(), totalToRevoke);

        log.info("[Point] 인증 삭제로 인한 포인트 회수. verificationId={}, userId={}, 회수 포인트={}",
                verification.getId(), user.getId(), totalToRevoke);
    }

    @Override
    @Transactional(readOnly = true)
    public PointHistoryResponseDto.PageDto getMyPointHistory(User user, int page, int size) {
        // 최근 3개월(당월 포함) 시작 시점 계산: 오늘이 7월이면 5월 1일 00:00부터
        LocalDateTime from = LocalDate.now()
                .minusMonths(RECENT_MONTHS - 1)
                .withDayOfMonth(1)
                .atStartOfDay();

        Pageable pageable = PageRequest.of(page, size);
        Slice<PointHistory> slice = pointHistoryRepository.findMyPointHistory(user.getId(), from, pageable);
        Slice<PointHistoryResponseDto.HistoryDto> dtoSlice = slice.map(pointConverter::toHistoryDto);

        return PointHistoryResponseDto.PageDto.builder()
                .totalPoints(user.getPoints())
                .history(new SliceResponseDto<>(dtoSlice))
                .build();
    }

    // 실제 포인트 적립 처리: PointAwardExecutor(REQUIRES_NEW)에 위임하여 독립 트랜잭션으로 처리
    private void awardPoint(User user, PointType type, Challenge challenge, Round round, RandomMission randomMission, Verification verification) {
        try {
            pointAwardExecutor.execute(user, type, challenge, round, randomMission, verification);
        } catch (DataIntegrityViolationException e) {
            // 동시 요청 등으로 인해 DB 유니크 제약에 걸린 경우 - 이미 지급된 것으로 간주하고 조용히 무시(멱등 처리)
            log.info("[Point] 유니크 제약으로 인해 중복 지급을 건너뜁니다. userId={}, type={}", user.getId(), type);
        }
    }

    private PointType resolveWeekPointType(int weekIndex) {
        return switch (weekIndex) {
            case 0 -> PointType.WEEK1_PERFECT;
            case 1 -> PointType.WEEK2_PERFECT;
            default -> PointType.WEEK3_PERFECT;
        };
    }

    private LocalDate findLastVerificationDay(Challenge challenge, LocalDate weekStart, LocalDate weekEnd) {
        List<ChallengeDays> verificationDays = challenge.getChallengeDays().stream()
                .map(ChallengeDayJoin::getDayOfWeek)
                .toList();

        LocalDate cursor = weekEnd;
        while (!cursor.isBefore(weekStart)) {
            if (verificationDays.contains(ChallengeDays.from(cursor.getDayOfWeek()))) {
                return cursor;
            }
            cursor = cursor.minusDays(1);
        }
        return null;
    }
}