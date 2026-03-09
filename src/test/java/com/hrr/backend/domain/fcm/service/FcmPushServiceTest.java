package com.hrr.backend.domain.fcm.service;

import com.google.firebase.messaging.*;
import com.hrr.backend.domain.fcm.repository.FcmTokenRepository;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.notification.entity.NotificationType;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.repository.NotificationSettingRepository;
import com.hrr.backend.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmPushServiceTest {

    @InjectMocks
    private FcmPushServiceImpl fcmPushService;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    private User user;
    private NotificationEvent event;
    private NotificationDelivery delivery;
    private NotificationType mockType;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        lenient().when(user.getId()).thenReturn(1L);

        // NotificationType 모킹 및 기본 설정
        mockType = mock(NotificationType.class);
        lenient().when(mockType.getTypeName()).thenReturn(NotificationTypeName.CHALLENGE_EXTENSION);
        lenient().when(mockType.isMandatory()).thenReturn(false); // 기본은 필수 알림 아님

        event = NotificationEvent.builder()
                .type(mockType)
                .category(NotificationCategory.CHALLENGE)
                .title("챌린지 알림")
                .message("테스트 메시지")
                .build();

        delivery = NotificationDelivery.builder()
                .event(event)
                .receiver(user)
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("활성 토큰이 있고 알림 설정이 켜진 경우 FCM 벌크 발송에 성공한다")
    void sendPushForDeliveries_success() throws FirebaseMessagingException {
        // given
        NotificationSetting setting = settingEnabled();
        given(notificationSettingRepository.findAllByUserIn(anyList())).willReturn(List.of(setting));
        given(fcmTokenRepository.findAllActiveTokensByUsers(anyList())).willReturn(List.of("token-1", "token-2"));

        BatchResponse batchResponse = mock(BatchResponse.class);
        given(batchResponse.getResponses()).willReturn(List.of());

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, times(1)).sendEachForMulticast(any(MulticastMessage.class));
            verify(fcmTokenRepository, times(1)).findAllActiveTokensByUsers(anyList());
        }
    }

    @Test
    @DisplayName("필수 알림(isMandatory=true)인 경우 유저 설정을 무시하고 발송한다")
    void sendPushForDeliveries_mandatory_ignoresSetting() throws FirebaseMessagingException {
        // given
        given(mockType.isMandatory()).willReturn(true);

        // 챌린지 알림이 꺼져있는 설정 객체
        NotificationSetting disabledSetting = NotificationSetting.builder()
                .user(user)
                .isChallengeEnabled(false)
                .build();

        given(notificationSettingRepository.findAllByUserIn(anyList())).willReturn(List.of(disabledSetting));
        given(fcmTokenRepository.findAllActiveTokensByUsers(anyList())).willReturn(List.of("mandatory-token"));

        BatchResponse batchResponse = mock(BatchResponse.class);
        given(batchResponse.getResponses()).willReturn(List.of());

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, times(1)).sendEachForMulticast(any());
        }
    }

    @Test
    @DisplayName("토큰이 500개(제한치)를 초과하면 파티셔닝하여 2번 발송한다")
    void sendPushForDeliveries_partitioning_success() throws FirebaseMessagingException {
        // given
        given(notificationSettingRepository.findAllByUserIn(anyList())).willReturn(List.of(settingEnabled()));

        // 501개의 토큰 생성
        List<String> largeTokens = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            largeTokens.add("token-" + i);
        }
        given(fcmTokenRepository.findAllActiveTokensByUsers(anyList())).willReturn(largeTokens);

        BatchResponse batchResponse = mock(BatchResponse.class);
        given(batchResponse.getResponses()).willReturn(List.of());

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            // 500개 단위로 나누어지므로 2회 호출 확인
            verify(firebaseMessaging, times(2)).sendEachForMulticast(any());
        }
    }

    @Test
    @DisplayName("UNREGISTERED 에러 발생 시 해당 토큰을 찾아 비활성화한다")
    void handleFailedTokens_deactivatesInvalidToken() throws FirebaseMessagingException {
        // given
        String invalidToken = "invalid-token";
        given(notificationSettingRepository.findAllByUserIn(anyList())).willReturn(List.of(settingEnabled()));
        given(fcmTokenRepository.findAllActiveTokensByUsers(anyList())).willReturn(List.of(invalidToken));

        // 실패 응답 구성
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);

        SendResponse failResponse = mock(SendResponse.class);
        given(failResponse.isSuccessful()).willReturn(false);
        given(failResponse.getException()).willReturn(exception);

        BatchResponse batchResponse = mock(BatchResponse.class);
        given(batchResponse.getResponses()).willReturn(List.of(failResponse));

        // 토큰 엔티티 모킹
        com.hrr.backend.domain.fcm.entity.FcmToken fcmTokenEntity = mock(com.hrr.backend.domain.fcm.entity.FcmToken.class);
        given(fcmTokenRepository.findByToken(invalidToken)).willReturn(Optional.of(fcmTokenEntity));

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(fcmTokenEntity, times(1)).deactivateToken();
            verify(fcmTokenRepository, times(1)).save(fcmTokenEntity);
        }
    }

    @Test
    @DisplayName("알림 설정이 아예 없는 유저는 기본적으로 발송 허용으로 간주한다")
    void sendPushForDeliveries_allowsWhenSettingIsNull() throws FirebaseMessagingException {
        // given
        given(notificationSettingRepository.findAllByUserIn(anyList())).willReturn(List.of()); // 빈 리스트 반환
        given(fcmTokenRepository.findAllActiveTokensByUsers(anyList())).willReturn(List.of("default-token"));

        BatchResponse batchResponse = mock(BatchResponse.class);
        given(batchResponse.getResponses()).willReturn(List.of());

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, times(1)).sendEachForMulticast(any());
        }
    }

    private NotificationSetting settingEnabled() {
        return NotificationSetting.builder()
                .user(user)
                .isChallengeEnabled(true)
                .isVerificationEnabled(true)
                .isFollowEnabled(true)
                .isBadgeEnabled(true)
                .build();
    }
}