package com.hrr.backend.domain.notification.event;

import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.user.entity.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ChallengeExtensionResponseEvent {
    private final Long roundId;
    private final User user;
    private final NextRoundIntent intent; // "계속하기" 또는 "그만하기"
}