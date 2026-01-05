package com.hrr.backend.domain.round.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.dto.RoundDecisionRequestDto;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoundDecisionServiceImpl implements RoundDecisionService {

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final RoundRecordRepository roundRecordRepository;

    @Override
    @Transactional
    public void decideNextRound(Long userId, Long challengeId, RoundDecisionRequestDto request) {

        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

        UserChallenge uc = userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_CHALLENGE_NOT_FOUND));

        if (uc.getStatus() != ChallengeJoinStatus.JOINED) {
            throw new GlobalException(ErrorCode.USER_CHALLENGE_NOT_FOUND);
        }

        Round currentRound = challenge.getCurrentRound();
        if (currentRound == null) {
            throw new GlobalException(ErrorCode.ROUND_NOT_FOUND);
        }

        LocalDate today = LocalDate.now();

        // 라운드 기간 체크
        if (today.isBefore(currentRound.getStartDate())) {
            throw new GlobalException(ErrorCode.ROUND_NOT_STARTED);
        }
        if (today.isAfter(currentRound.getEndDate())) {
            throw new GlobalException(ErrorCode.ROUND_ALREADY_ENDED);
        }

        // 결정 기간 체크: 종료 3일 전(포함)부터 가능
        LocalDate decisionOpenDate = currentRound.getEndDate().minusDays(Challenge.CHALLENGER_DECISION_DAYS);
        if (today.isBefore(decisionOpenDate)) {
            throw new GlobalException(ErrorCode.ROUND_DECISION_PERIOD_NOT_OPEN);
        }

        // intent 유효성 (UNDECIDED로 요청하는 것 방지)
        NextRoundIntent intent = request.intent();
        if (intent == null || intent == NextRoundIntent.UNDECIDED) {
            throw new GlobalException(ErrorCode.ROUND_DECISION_INTENT_INVALID);
        }

        RoundRecord rr = roundRecordRepository.findByUserChallengeAndRoundId(uc, currentRound.getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.ROUND_RECORD_NOT_FOUND));

        rr.updateNextRoundIntent(intent);
        // rr은 영속 상태라 save 없어도 되지만, 명시적으로 해도 OK
        // roundRecordRepository.save(rr);
    }
}
