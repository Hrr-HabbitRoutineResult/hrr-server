package com.hrr.backend.domain.round.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class RoundLifecycleServiceImpl implements RoundLifecycleService {

    private final RoundRepository roundRepository;
    private final RoundRecordRepository roundRecordRepository;
    private final ChallengeRepository challengeRepository;
    private final RoundConverter roundConverter;

    @Override
    @Transactional
    public void processRoundsEndedAt(LocalDate endDate) {

        List<Round> endedRounds = roundRepository.findAllByEndDate(endDate);

        for (Round endedRound : endedRounds) {
            try {
                processSingleEndedRound(endedRound);
            } catch (Exception e) {
                log.error("[RoundLifecycle] 라운드 종료 처리 실패. roundId={}", endedRound.getId(), e);
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

        //2. 진행중인 챌린지만 라운드 자동 전환
        if (challenge.getStatus() != ChallengeStatus.ONGOING) {
            return;
        }

        // 3. 현재 라운드의 참가자 기록 조회 (JOINED만)
        List<RoundRecord> records = roundRecordRepository.findAllByRoundWithUserAndSetting(
                endedRound,
                ChallengeJoinStatus.JOINED
        );

        // 4. CONTINUE가 1명이라도 있으면 챌린지는 계속 (방장 STOP이어도 상관없개)
        List<RoundRecord> continuers = records.stream()
                .filter(rr -> rr.getNextRoundIntent() == NextRoundIntent.CONTINUE)
                .toList();

        if (continuers.isEmpty()) {
            // 아무도 연장 안 함(UNDECIDED 포함 자동 하차) => 챌린지 종료
            finishChallengeAndDropAll(challenge, records);
            return;
        }

        // 5. 다음 라운드 생성(이미 존재하면 재사용)
        Round nextRound = roundRepository
                .findByChallengeIdAndRoundNumber(challenge.getId(), endedRound.getRoundNumber() + 1)
                .orElseGet(() -> roundRepository.save(roundConverter.toNextRoundEntity(challenge, endedRound)));

        // 6. CONTINUE는 nextRound로 RoundRecord 생성
        for (RoundRecord rr : continuers) {
            UserChallenge uc = rr.getUserChallenge();

            // 멱등성- 이미 다음 라운드 기록이 있으면 스킵
            if (roundRecordRepository.existsByUserChallengeAndRound(uc, nextRound)) {
                continue;
            }
            RoundRecord nextRR = roundConverter.toRoundRecordEntity(nextRound, uc);
            roundRecordRepository.save(nextRR);
        }

        // 7. STOP/UNDECIDED 는 하차 처리
        for (RoundRecord rr : records) {
            if (rr.getNextRoundIntent() == NextRoundIntent.CONTINUE) continue;

            UserChallenge uc = rr.getUserChallenge();
            uc.updateStatus(ChallengeJoinStatus.DROPPED);
            challengeRepository.decreaseCurrentParticipantCount(challenge.getId());
        }

        // 8. 챌린지 currentRound 교체
        challenge.changeCurrentRound(nextRound);

        log.info("[RoundLifecycle] 라운드 전환 완료. challengeId={}, {}R -> {}R",
                challenge.getId(), endedRound.getRoundNumber(), nextRound.getRoundNumber());
    }

    private void finishChallengeAndDropAll(Challenge challenge, List<RoundRecord> records) {

        // 전원 하차 처리
        for (RoundRecord rr : records) {
            UserChallenge uc = rr.getUserChallenge();
            if (uc.getStatus() == ChallengeJoinStatus.JOINED) {
                uc.updateStatus(ChallengeJoinStatus.DROPPED);
                challengeRepository.decreaseCurrentParticipantCount(challenge.getId());
            }
        }

        // 챌린지 종료(상태 변경)
        // 엔티티 메서드가 없다면 repository update 쿼리 추가해서 처리해도 됨.
        challenge.updateStatus(ChallengeStatus.FINISHED);

        log.info("[RoundLifecycle] 챌린지 종료 처리. challengeId={}", challenge.getId());
    }
}
