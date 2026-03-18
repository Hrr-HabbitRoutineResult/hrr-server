package com.hrr.backend.domain.fcm.listener;

import com.hrr.backend.domain.fcm.event.FcmPushSendEvent;
import com.hrr.backend.domain.fcm.service.FcmPushService;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmPushEventListenerTest {

    @InjectMocks
    private FcmPushEventListener fcmPushEventListener;

    @Mock
    private FcmPushService fcmPushService;

    @Test
    @DisplayName("FcmPushSendEvent 수신 시 FcmPushService의 발송 메서드가 호출되어야 한다")
    void handleFcmPushSendEvent_Success() {
        // given
        NotificationEvent mockEvent = mock(NotificationEvent.class);
        NotificationDelivery mockDelivery1 = mock(NotificationDelivery.class);
        NotificationDelivery mockDelivery2 = mock(NotificationDelivery.class);
        List<NotificationDelivery> deliveries = List.of(mockDelivery1, mockDelivery2);

        FcmPushSendEvent pushEvent = new FcmPushSendEvent(deliveries, mockEvent);

        // when
        fcmPushEventListener.handleFcmPushSendEvent(pushEvent);

        // then
        // 리스너가 이벤트를 받아서 서비스의 발송 메서드를 정확한 인자와 함께 호출하는지 검증
        verify(fcmPushService, times(1)).sendPushForDeliveries(deliveries, mockEvent);
    }
}