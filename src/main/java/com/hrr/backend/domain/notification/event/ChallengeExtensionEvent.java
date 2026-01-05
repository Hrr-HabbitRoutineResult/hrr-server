package com.hrr.backend.domain.notification.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ChallengeExtensionEvent {
    private final Long roundId;
}