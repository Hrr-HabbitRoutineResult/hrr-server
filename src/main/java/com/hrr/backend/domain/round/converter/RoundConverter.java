package com.hrr.backend.domain.round.converter;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.user.entity.UserChallenge;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class RoundConverter {
    /**
     * 챌린지 생성 시, 1회차 라운드(Round) 엔티티를 생성
     * 종료일(endDate)은 시작일로부터 3주 뒤로 자동 계산
     */
    public Round toFirstRoundEntity(Challenge challenge, LocalDate startDate) {
        return Round.builder()
                .challenge(challenge)
                .roundNumber(1)
                .startDate(startDate)
                .endDate(startDate.plusWeeks(Challenge.ROUND_WEEKS).minusDays(1))
                .build();
    }

    /**
     * 초기화된(점수 0, 연장 여부 미정) RoundRecord 엔티티 생성
     */
    public RoundRecord toRoundRecordEntity(Round round, UserChallenge userChallenge) {
        return RoundRecord.builder()
                .round(round)
                .userChallenge(userChallenge)
                .verificationCount(0)
                .warnCount(0)
                .nextRoundIntent(NextRoundIntent.UNDECIDED)
                .build();
    }

    /** next round 생성 메서드(다음 라운드를 호출할 수 있게) */
    public Round toNextRoundEntity(Challenge challenge, Round prevRound) {
        LocalDate nextStart = prevRound.getEndDate().plusDays(1);
        return Round.builder()
                .challenge(challenge)
                .roundNumber(prevRound.getRoundNumber() + 1)
                .startDate(nextStart)
                .endDate(nextStart.plusWeeks(Challenge.ROUND_WEEKS).minusDays(1))
                .build();
    }

}