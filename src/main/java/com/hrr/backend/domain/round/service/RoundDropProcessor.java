package com.hrr.backend.domain.round.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoundDropProcessor {

    private final RoundRecordRepository roundRecordRepository;
    private final ChallengeRepository challengeRepository;

    @Transactional
    public void processRound(Round round) {
        Challenge challenge = round.getChallenge();

        // 진행중 챌린지만 처리
        if (challenge.getStatus() != ChallengeStatus.ONGOING) return;

        // 멱등성: 이미 currentRound가 바뀌었으면 스킵
        if (challenge.getCurrentRound() == null || !challenge.getCurrentRound().getId().equals(round.getId())) {
            return;
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
    }
}
