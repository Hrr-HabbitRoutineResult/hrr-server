package com.hrr.backend.domain.notification.event;

public record CommentCreatedEvent(
        Long verificationId,
        Long commentId,
        Long actorId
) {
}
