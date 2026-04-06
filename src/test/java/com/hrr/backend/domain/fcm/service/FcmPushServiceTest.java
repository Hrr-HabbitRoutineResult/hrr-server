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
import com.hrr.backend.global.s3.S3UrlUtil;
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
    private FcmTokenDeactivationService fcmTokenDeactivationService; // 신규 서비스 주입

    @Mock
    private NotificationSettingRepository notificationSettingRepository;

    @Mock
    private S3UrlUtil s3UrlUtil;

    private User user;
    private NotificationEvent event;
    private NotificationDelivery delivery;
    private NotificationType mockType;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        lenient().when(user.getId()).thenReturn(1L);

        lenient().when(s3UrlUtil.toFullUrl(any()))
                .thenAnswer(invocation -> "https://s3.amazonaws.com/" + invocation.getArgument(0));

        mockType = mock(NotificationType.class);
        lenient().when(mockType.getTypeName()).thenReturn(NotificationTypeName.CHALLENGE_EXTENSION);
        lenient().when(mockType.isMandatory()).thenReturn(false);

        event = NotificationEvent.builder()
                .type(mockType)
                .category(NotificationCategory.CHALLENGE)
                .title("챌린지 알림")
                .message("테스트 메시지")
                .imageKey("challenges/test.png")
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
        given(notificationSettingRepository.findAllByUserIn(anyList())).willReturn(List.of(settingEnabled()));
        given(fcmTokenRepository.findAllActiveTokensByUsers(anyList())).willReturn(List.of("token-1", "token-2"));

        BatchResponse batchResponse = mock(BatchResponse.class);
        // MulticastMessage 전송 성공 시나리오 모킹
        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, times(1)).sendEachForMulticast(any());
            // 비활성화 서비스가 호출되는지 확인 (에러 유무 상관없이 호출 로직 실행됨)
            verify(fcmTokenDeactivationService, times(1)).handleFailedTokens(anyList(), any());
        }
    }

    @Test
    @DisplayName("필수 알림(isMandatory=true)인 경우 유저 설정을 무시하고 발송한다")
    void sendPushForDeliveries_mandatory_ignoresSetting() throws FirebaseMessagingException {
        // given
        given(mockType.isMandatory()).willReturn(true);
        given(fcmTokenRepository.findAllActiveTokensByUsers(anyList())).willReturn(List.of("mandatory-token"));

        BatchResponse batchResponse = mock(BatchResponse.class);
        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, times(1)).sendEachForMulticast(any());
            verify(fcmTokenDeactivationService, times(1)).handleFailedTokens(anyList(), any());
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
        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, times(2)).sendEachForMulticast(any());
            // 파티션이 2개이므로 서비스도 2번 호출되어야 함
            verify(fcmTokenDeactivationService, times(2)).handleFailedTokens(anyList(), any());
        }
    }

    @Test
    @DisplayName("FCM 발송 후 결과에 대해 비활성화 서비스를 호출한다")
    void shouldCallDeactivationService_regardlessOfError() throws FirebaseMessagingException {
        // given
        given(notificationSettingRepository.findAllByUserIn(anyList())).willReturn(List.of(settingEnabled()));
        given(fcmTokenRepository.findAllActiveTokensByUsers(anyList())).willReturn(List.of("token-1"));

        BatchResponse batchResponse = mock(BatchResponse.class);
        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            // Repository를 호출하지 않고 서비스를 호출
            verify(fcmTokenDeactivationService, times(1)).handleFailedTokens(anyList(), any());
            verify(fcmTokenRepository, never()).deactivateAllByToken(anyString());
        }
    }

    @Test
    @DisplayName("알림 설정이 아예 없는 유저는 기본적으로 발송 허용으로 간주한다")
    void sendPushForDeliveries_allowsWhenSettingIsNull() throws FirebaseMessagingException {
        // given
        given(notificationSettingRepository.findAllByUserIn(anyList())).willReturn(List.of());
        given(fcmTokenRepository.findAllActiveTokensByUsers(anyList())).willReturn(List.of("default-token"));

        BatchResponse batchResponse = mock(BatchResponse.class);
        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any())).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, times(1)).sendEachForMulticast(any());
            verify(fcmTokenDeactivationService, times(1)).handleFailedTokens(anyList(), any());
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