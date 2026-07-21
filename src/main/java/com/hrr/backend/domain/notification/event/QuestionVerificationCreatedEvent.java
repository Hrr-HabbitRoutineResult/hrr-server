package com.hrr.backend.domain.notification.event;

public record QuestionVerificationCreatedEvent(
        Long verificationId,
        Long actorId
) {
}
