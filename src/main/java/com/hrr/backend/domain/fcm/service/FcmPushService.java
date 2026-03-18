package com.hrr.backend.domain.fcm.service;

import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;

import java.util.List;

public interface FcmPushService {

    // 여러 수신자에게 FCM 푸시 발송 (챌린지 연장 안내 등 다수 발송용)
    void sendPushForDeliveries(List<NotificationDelivery> deliveries, NotificationEvent event);

    // 단일 수신자에게 FCM 푸시 발송 (연장 응답 결과 등 1:1 발송용)
    void sendPushForDelivery(NotificationDelivery delivery, NotificationEvent event);
}