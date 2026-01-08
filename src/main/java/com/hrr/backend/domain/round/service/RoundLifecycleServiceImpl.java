package com.hrr.backend.domain.round.service;

import java.time.LocalDate;
import java.util.List;

import com.hrr.backend.domain.challenge.service.ChallengeStaticsService;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.round.converter.RoundConverter;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.global.common.enums.ChallengeStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoundLifecycleServiceImpl implements RoundLifecycleService {

    private final RoundRepository roundRepository;
    private final RoundRecordRepository roundRecordRepository;
    private final RoundConverter roundConverter;
    private final TransactionTemplate transactionTemplate;

    private final ChallengeStaticsService challengeStaticsService;

    @Override
    public void processRoundsEndedAt(LocalDate endDate) {

        List<Round> endedRounds = roundRepository.findAllByEndDate(endDate);

        for (Round endedRound : endedRounds) {
            Long endedRoundId = endedRound.getId();

            try {
                transactionTemplate.executeWithoutResult(status -> {
                    //트랜잭션 내부에서 Round를 다시 조회해서(detached 방지) 처리
                    Round managedEndedRound = roundRepository.findByIdWithChallengeAndCurrentRound(endedRoundId)
                            .orElseThrow(() -> new IllegalStateException("Round not found. id=" + endedRoundId));

                    processSingleEndedRound(managedEndedRound);
                });
            } catch (Exception e) {
                log.error("[RoundLifecycle] 라운드 종료 처리 실패. roundId={}", endedRoundId, e);
            }
        }
    }

    private void processSingleEndedRound(Round endedRound) {

        Challenge challenge = endedRound.getChallenge();

        // 1. 멱등성: 현재 라운드가 이미 교체됐으면 스킵
        Round currentRound = challenge.getCurrentRound();
        if (currentRound == null || !currentRound.getId().equals(endedRound.getId())) {
            return;
        }

        // 2. 진행중인 챌린지만 라운드 자동 전환
        if (challenge.getStatus() != ChallengeStatus.ONGOING) {
            return;
        }

        // 3. 현재 라운드의 참가자 기록 조회 (JOINED만)
        //STOP/UNDECIDED는 S-1일 23:59 드랍 스케줄러가 먼저 DROPPED 처리해야 함
        List<RoundRecord> records = roundRecordRepository.findAllByRoundWithUserAndSetting(
                endedRound,
                ChallengeJoinStatus.JOINED
        );

        // 4. 다음 라운드로 넘어갈 사람 = (드랍 이후 JOINED 중) CONTINUE인 사람
        List<RoundRecord> continuers = records.stream()
                .filter(rr -> rr.getNextRoundIntent() == NextRoundIntent.CONTINUE)
                .toList();

        if (continuers.isEmpty()) {
            // 남아있는 연장자가 없으면 챌린지 종료(드랍은 별도 23:59 배치가 담당)
            finishChallengeOnly(challenge);
            return;
        }

        // 5. 다음 라운드 생성
        Round nextRound = roundRepository
                .findByChallengeIdAndRoundNumber(challenge.getId(), endedRound.getRoundNumber() + 1)
                .orElseGet(() -> roundRepository.save(roundConverter.toNextRoundEntity(challenge, endedRound)));

        // 6. CONTINUE는 nextRound로 RoundRecord 생성
        for (RoundRecord rr : continuers) {
            UserChallenge uc = rr.getUserChallenge();

            // 멱등성: 이미 다음 라운드 기록이 있으면 스킵
            if (roundRecordRepository.existsByUserChallengeAndRound(uc, nextRound)) {
                continue;
            }

            RoundRecord nextRR = roundConverter.toRoundRecordEntity(nextRound, uc);
            roundRecordRepository.save(nextRR);
        }

        // 7. 챌린지 currentRound 교체
        challenge.changeCurrentRound(nextRound);

        // 8. 통계 최신화 1회
        challengeStaticsService.updateChallengeStatics(challenge);

        log.info("[RoundLifecycle] 라운드 전환 완료. challengeId={}, {}R -> {}R",
                challenge.getId(), endedRound.getRoundNumber(), nextRound.getRoundNumber());
    }

    private void finishChallengeOnly(Challenge challenge) {
        challenge.updateStatus(ChallengeStatus.FINISHED);
        log.info("[RoundLifecycle] 챌린지 종료 처리(FINISHED). challengeId={}", challenge.getId());
    }
}
