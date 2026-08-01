package com.hrr.backend.domain.point.service;

import java.time.LocalDate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.global.util.ChallengeVerificationWindowUtil;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.point.converter.PointConverter;
import com.hrr.backend.domain.point.dto.PointHistoryResponseDto;
import com.hrr.backend.domain.point.entity.PointHistory;
import com.hrr.backend.domain.point.entity.enums.PointType;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
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
import com.hrr.backend.domain.verification.repository.VerificationRepository;
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
    private final VerificationRepository verificationRepository;
    private final Clock clock;

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
        // CANCELLED는 나가기 시 RoundRecord가 삭제되어 애초에 조회되지 않음
        // 종료된 라운드에 속한 모든 RoundRecord 조회 (JOINED/DROPPED 대상)
        List<RoundRecord> records = roundRecordRepository.findAllByRoundIdWithUserAndChallenge(endedRound.getId());

        for (RoundRecord record : records) {
            try {
                // 회원 탈퇴로 종료된(KICKED) 참여자는 무결석 완주 대상에서 제외
                if (record.getUserChallenge().getStatus() == ChallengeJoinStatus.KICKED) {
                    continue;
                }

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
        LocalDate verifiedDate = ChallengeVerificationWindowUtil.getWindowAnchorDate(challenge, verifiedAt);
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
        // 비관적 락으로 조회해서, 동시에 두 번 삭제 요청이 들어와도 포인트가 중복 차감되지 않도록 직렬화
        List<PointHistory> histories = pointHistoryRepository.findAllByVerificationIdForUpdate(verification.getId());

        if (histories.isEmpty()) {
            return; // 이 인증글로 지급된 포인트가 없으면 할 일 없음
        }

        long totalToRevoke = histories.stream()
                .mapToLong(PointHistory::getPoints)
                .sum();

        User user = verification.getRoundRecord().getUserChallenge().getUser();

        // point_history 먼저 삭제(FK 제약상 verification 삭제보다 먼저 처리되어야 함) + 유저 포인트 원자적 차감
        // 이 메서드는 인증 삭제와 같은 트랜잭션 안에서 호출되어, 인증 삭제와 포인트 회수가 함께 성공/실패한다.
        pointHistoryRepository.deleteAll(histories);
        userRepository.decreasePoints(user.getId(), totalToRevoke);

        log.info("[Point] 인증 삭제로 인한 포인트 회수. verificationId={}, userId={}, 회수 포인트={}",
                verification.getId(), user.getId(), totalToRevoke);
    }

    /**
     * 인증글 생성 트랜잭션이 커밋된 이후(AFTER_COMMIT)에 호출되는 진입점.
     * verificationId로 인증글을 다시 조회한 뒤, 첫 인증 포인트 + 주차 퍼펙트 포인트 지급 로직을 그대로 재사용한다.
     * 이 시점엔 인증글이 이미 커밋되어 있어서, point_history.verification_id FK 저장 시
     * (REQUIRES_NEW로 분리된) PointAwardExecutor가 아직 커밋되지 않은 부모 행을 기다리며
     * 락 대기/타임아웃에 빠지는 문제가 없다.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void awardVerificationTriggeredPoints(Long verificationId) {
        Verification verification = verificationRepository.findById(verificationId).orElse(null);
        if (verification == null) {
            // 매우 드문 케이스(조회 사이 삭제 등) - 조용히 무시
            log.warn("[Point] 포인트 지급 대상 인증글을 찾을 수 없습니다. verificationId={}", verificationId);
            return;
        }

        RoundRecord roundRecord = verification.getRoundRecord();
        UserChallenge userChallenge = roundRecord.getUserChallenge();
        User user = userChallenge.getUser();
        Challenge challenge = userChallenge.getChallenge();
        Round round = roundRecord.getRound();

        earnFirstVerificationPoint(user, challenge, verification);
        checkAndEarnWeeklyPerfectPoint(userChallenge, roundRecord, round, challenge, verification.getCreatedAt(), verification);
    }

    @Override
    @Transactional(readOnly = true)
    public PointHistoryResponseDto.PageDto getMyPointHistory(User user, int page, int size) {
        // 최근 3개월(당월 포함) 시작 시점 계산: 오늘이 7월이면 5월 1일 00:00부터
        // 서버 기본 타임존에 의존하지 않고 KST 기준으로 계산되도록, 그리고 테스트에서 시각을 고정할 수 있도록 변경
        LocalDateTime from = LocalDate.now(clock)
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
            // 지급 직전에 대상 인증글이 삭제되어 FK 제약에 걸린 경우 -> 지급할 대상이 사라졌으므로 무시
            log.info("[Point] 포인트 지급을 건너뜁니다(이미 지급됐거나 대상 인증글이 삭제됨). userId={}, type={}",
                    user.getId(), type);
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