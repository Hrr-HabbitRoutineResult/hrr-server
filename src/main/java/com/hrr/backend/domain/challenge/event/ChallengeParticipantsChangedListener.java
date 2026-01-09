package com.hrr.backend.domain.challenge.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeParticipantsChangedListener {

    private final ChallengeStaticsUpdater challengeStaticsUpdater;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onParticipantsChanged(ChallengeParticipantsChangedEvent e) {
        try {
            challengeStaticsUpdater.recalculateInNewTx(e.challengeId());
        } catch (Exception ex) {
            // 라운드는 이미 커밋됨. 여기서 예외 터져도 라운드 전환은 유지됨.
            log.error("[Statics] update failed after commit. challengeId={}", e.challengeId(), ex);
        }
    }
}
