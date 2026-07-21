package com.hrr.backend.domain.notification.event;

public record WeakVerificationWarningEvent(
        Long verificationId,
        Long warnedUserId
) {
}
