package com.hrr.backend.domain.fcm.listener;

import com.hrr.backend.domain.fcm.event.FcmPushSendEvent;
import com.hrr.backend.domain.fcm.service.FcmPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FcmPushEventListener {

    private final FcmPushService fcmPushService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFcmPushSendEvent(FcmPushSendEvent event) {
        fcmPushService.sendPushForDeliveries(event.deliveries(), event.notificationEvent());
        log.info("DB 커밋 후 FCM 푸시 발송 완료: 대상={}건", event.deliveries().size());
    }
}