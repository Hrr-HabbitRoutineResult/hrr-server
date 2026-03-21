package com.hrr.backend.domain.notification.event;

import java.time.LocalDateTime;

public record VerificationDeadlineEvent(
        Long roundId,
        Long challengeId,
        LocalDateTime startAt,
        LocalDateTime endAt
) {}
