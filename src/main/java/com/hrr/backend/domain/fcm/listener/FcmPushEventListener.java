package com.hrr.backend.domain.fcm.listener;

import com.hrr.backend.domain.fcm.event.FcmPushSendEvent;
import com.hrr.backend.domain.fcm.service.FcmPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FcmPushEventListener {

    private final FcmPushService fcmPushService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFcmPushSendEvent(FcmPushSendEvent event) {
        fcmPushService.sendPushForDeliveries(event.deliveries(), event.notificationEvent());
    }
}
