package com.hrr.backend.domain.challenge.event;

public record ChallengeCreatedEvent(
        Long challengeId,
        String challengeText
) {
}