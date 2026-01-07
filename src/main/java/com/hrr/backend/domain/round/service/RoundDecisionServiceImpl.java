package com.hrr.backend.domain.round.service;

import java.time.LocalDate;
import java.time.ZoneId;

import com.hrr.backend.domain.notification.event.ChallengeExtensionResponseEvent;
import com.hrr.backend.domain.round.dto.RoundDecisionResponseDto;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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

        // 알림 이벤트 발행
        // 비동기 리스너가 이 이벤트를 받아 SUCCESS 또는 CANCEL 알림을 생성
        eventPublisher.publishEvent(new ChallengeExtensionResponseEvent(
                currentRound.getId(),
                uc.getUser(),
                intent
        ));
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
         * endDate - 2일 (Open) ~ endDate - 1일 (Close)
         * 예: 라운드 종료일이 10일이면, 8일(Open) ~ 9일(Close 전까지? 로직상 8일 하루만 가능해보임)
         * 기존 로직 유지하되 가독성 개선:
         * decisionOpenDate = D-2
         * decisionCloseDate = D-1 (이 날짜가 되면 닫힘? 혹은 이 날짜까지 가능?)
         * 원본 로직: !today.isBefore(decisionCloseDate) -> today >= decisionCloseDate 이면 에러.
         * 즉, [D-2] 하루만 열리는 매우 타이트한 기간임.
         */
        LocalDate decisionOpenDate = endDate.minusDays(2);
        LocalDate decisionCloseDate = endDate.minusDays(1);

        if (today.isBefore(decisionOpenDate)) {
            throw new GlobalException(ErrorCode.ROUND_DECISION_PERIOD_NOT_OPEN);
        }
        // closeDate(D-1)가 되는 순간 기간 만료 처리 (D-2 하루만 가능)
        if (!today.isBefore(decisionCloseDate)) {
            throw new GlobalException(ErrorCode.ROUND_DECISION_PERIOD_CLOSED); // 에러코드 명칭 명확화 권장
        }
    }
}
