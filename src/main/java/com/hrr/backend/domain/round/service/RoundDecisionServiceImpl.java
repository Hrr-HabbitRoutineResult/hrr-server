package com.hrr.backend.domain.round.service;

import java.time.LocalDate;
import java.time.ZoneId;

import com.hrr.backend.domain.round.dto.RoundDecisionResponseDto;
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
    public RoundDecisionResponseDto decideNextRound(Long userId, Long challengeId, RoundDecisionRequestDto request) {

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

        // 결정 기간 검증
        validateDecisionPeriod(currentRound);

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

        // 응답 반환 (isResponded = true 포함)
        return RoundDecisionResponseDto.of(currentRound.getId(), intent);
    }

    private void validateDecisionPeriod(Round currentRound) {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        LocalDate endDate = currentRound.getEndDate();

        // 라운드 진행 기간 체크
        if (today.isBefore(currentRound.getStartDate())) {
            throw new GlobalException(ErrorCode.ROUND_NOT_STARTED);
        }
        if (today.isAfter(endDate)) {
            throw new GlobalException(ErrorCode.ROUND_ALREADY_ENDED);
        }

        /*
         * 결정 기간 로직:
         * [변경 후]
         * - decisionOpenDate = endDate - 3일 (D-3, 18일)
         * - decisionCloseDate = endDate - 2일 (D-2, 19일)
         * - 결과: D-3 ~ D-2 (2일간) 가능
         *
         * [타임라인 예시] endDate = 21일
         * - 18일(D-3) 09:00: 알림 발송
         * - 18일 ~ 19일 23:59: 연장 여부 응답 가능
         * - 20일 23:59: 무응답자 드랍
         * - 21일 00:10: 라운드 전환
         */
        LocalDate decisionOpenDate = endDate.minusDays(3);  // 수정: D-2 → D-3
        LocalDate decisionCloseDate = endDate.minusDays(2); //수정: D-1 → D-2

        // D-3 이전에는 아직 열리지 않음
        if (today.isBefore(decisionOpenDate)) {
            throw new GlobalException(ErrorCode.ROUND_DECISION_PERIOD_NOT_OPEN);
        }

        // D-2를 넘어가면 기간 만료 (D-2 23:59까지 가능)
        // !today.isBefore(decisionCloseDate) → today.isAfter(decisionCloseDate)
        // 이유: D-2 당일까지 포함시키기 위함
        if (today.isAfter(decisionCloseDate)) {
            throw new GlobalException(ErrorCode.ROUND_DECISION_PERIOD_CLOSED);
        }
    }
}
