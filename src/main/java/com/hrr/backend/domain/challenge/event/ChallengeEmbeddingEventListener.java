package com.hrr.backend.domain.challenge.event;

import com.hrr.backend.domain.challenge.service.ChallengeEmbeddingAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeEmbeddingEventListener {

    private final ChallengeEmbeddingAsyncService challengeEmbeddingAsyncService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChallengeCreated(ChallengeCreatedEvent event) {
        Long challengeId = event.challengeId();
        String challengeText = event.challengeText();

        challengeEmbeddingAsyncService.calculateAndSaveEmbedding(
                challengeId,
                challengeText
        );
    }
}
