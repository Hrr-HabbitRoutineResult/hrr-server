package com.hrr.backend.domain.fcm.event;

import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import java.util.List;

/**
 * DB 커밋 후 FCM 발송을 트리거하기 위한 불변 데이터 객체
 */
public record FcmPushSendEvent(
        List<NotificationDelivery> deliveries,
        NotificationEvent notificationEvent
) {
}