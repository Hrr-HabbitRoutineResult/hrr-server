package com.hrr.backend.domain.round.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoundDropServiceImpl implements RoundDropService {

    private final RoundRepository roundRepository;
    private final RoundRecordRepository roundRecordRepository;
    private final ChallengeRepository challengeRepository;

    @Override
    @Transactional
    public void dropNonContinuersAt(LocalDate endDate) {

        // 오늘이 endDate인 라운드들 => 오늘 23:59에 드랍 처리 대상
        List<Round> rounds = roundRepository.findAllByEndDate(endDate);

        for (Round round : rounds) {
            try {
                Challenge challenge = round.getChallenge();

                // 진행중 챌린지만 처리
                if (challenge.getStatus() != ChallengeStatus.ONGOING) continue;

                // 멱등성: 이미 currentRound가 바뀌었으면 스킵
                if (challenge.getCurrentRound() == null || !challenge.getCurrentRound().getId().equals(round.getId())) {
                    continue;
                }

                // JOINED만 조회
                List<RoundRecord> records = roundRecordRepository.findAllByRoundWithUserAndSetting(
                        round,
                        ChallengeJoinStatus.JOINED
                );

                for (RoundRecord rr : records) {
                    if (rr.getNextRoundIntent() == NextRoundIntent.CONTINUE) continue;

                    UserChallenge uc = rr.getUserChallenge();
                    if (uc.getStatus() != ChallengeJoinStatus.JOINED) continue;

                    int decreasedCount = challengeRepository.decreaseCurrentParticipantCount(challenge.getId());
                    if (decreasedCount == 0) {
                        throw new GlobalException(ErrorCode.CHALLENGE_PARTICIPANT_COUNT_UNDERFLOW);
                    }

                    uc.updateStatus(ChallengeJoinStatus.DROPPED);
                }

            } catch (Exception e) {
                log.error("[RoundDrop] 드랍 처리 실패. roundId={}", round.getId(), e);
            }
        }
    }
}
