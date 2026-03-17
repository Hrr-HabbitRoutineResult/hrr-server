package com.hrr.backend.domain.notification.event;

import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.user.entity.User;

/**
 * 챌린지 연장 응답 결과를 담는 레코드
 */
public record ChallengeExtensionResponseEvent(
        Long roundId,
        User user,
        NextRoundIntent intent
) {
}