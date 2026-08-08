package com.hrr.backend.domain.fcm.listener;

import com.hrr.backend.domain.fcm.event.FcmPushSendEvent;
import com.hrr.backend.domain.fcm.service.FcmPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class FcmPushEventListener {

    private final FcmPushService fcmPushService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFcmPushSendEvent(FcmPushSendEvent event) {
        fcmPushService.sendPushForDeliveries(event.deliveries(), event.notificationEvent());
        log.info("[handleFcmPushSendEvent] 트랜잭션 commit 후 FCM 발송 이벤트 처리를 종료했습니다. deliveryCount={}, typeName={}",
                event.deliveries() != null ? event.deliveries().size() : 0,
                event.notificationEvent() != null && event.notificationEvent().getType() != null
                        ? event.notificationEvent().getType().getTypeName() : null);
    }
}
