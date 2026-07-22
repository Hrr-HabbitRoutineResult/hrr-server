package com.hrr.backend.domain.point.event;

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

    // 인증글 생성 트랜잭션이 실제로 커밋된 이후에만 실행된다.
    // 여기서 예외가 나도 이미 커밋된 인증글 생성 자체에는 영향을 주지 않는다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVerificationCreated(VerificationPointTriggerEvent event) {
        try {
            pointService.awardVerificationTriggeredPoints(event.verificationId());
        } catch (Exception e) {
            log.error("[Point] 인증 커밋 이후 포인트 지급 처리 실패. verificationId={}", event.verificationId(), e);
        }
    }
}