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
}