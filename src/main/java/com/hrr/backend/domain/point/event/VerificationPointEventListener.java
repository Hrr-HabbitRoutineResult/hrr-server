package com.hrr.backend.domain.point.event;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.hrr.backend.domain.point.service.PointService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationPointEventListener {

    private final PointService pointService;

    /** 인증글 생성 트랜잭션이 실제로 커밋된 이후에만 실행
     * 기존에 있던 try-catch는 제거했다. 예외를 삼켜버리면 @Retryable이 예외를 볼 수 없어 재시도 자체가 동작하지 않기 때문에, 예외를 밖으로 던짐
     */
    @Async
    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationCreated(VerificationPointTriggerEvent event) {
        pointService.awardVerificationTriggeredPoints(event.verificationId());

    }

    /** 재시도를 모두 소진하고도 실패한 경우 호출 */
    @Recover
    public void recover(Exception e, VerificationPointTriggerEvent event) {
        log.error("[recover] 인증 commit 후 포인트 지급 재시도를 모두 실패했습니다. verificationId={}",
                event.verificationId(), e);
    }
}
