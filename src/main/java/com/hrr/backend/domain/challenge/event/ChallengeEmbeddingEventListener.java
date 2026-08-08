package com.hrr.backend.domain.challenge.event;

import com.hrr.backend.domain.challenge.service.ChallengeEmbeddingAsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
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
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChallengeCreated(ChallengeCreatedEvent event) {
        Long challengeId = event.challengeId();
        String challengeText = event.challengeText();

        challengeEmbeddingAsyncService.calculateAndSaveEmbedding(
                challengeId,
                challengeText
        );
    }
    @Recover
    public void recover(Exception e, ChallengeCreatedEvent event) {
        Long challengeId = event.challengeId();
        String challengeText = event.challengeText();

        log.error("[recover] 임베딩 계산 재시도를 모두 실패했습니다. challengeId={}", challengeId, e);

        // 최종 실패 시 768 길이 0벡터로 강제 저장
        challengeEmbeddingAsyncService.saveZeroEmbeddingFallback(challengeId, challengeText);
        log.warn("[recover] 임베딩 계산 실패에 대한 zero embedding fallback 저장을 완료했습니다. challengeId={}", challengeId);
    }
}
