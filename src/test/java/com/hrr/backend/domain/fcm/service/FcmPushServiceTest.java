package com.hrr.backend.domain.fcm.service;

import com.google.firebase.messaging.*;
import com.hrr.backend.domain.fcm.entity.FcmToken;
import com.hrr.backend.domain.fcm.repository.FcmTokenRepository;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.notification.entity.NotificationType;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.entity.enums.ResourceType;
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

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        lenient().when(user.getId()).thenReturn(1L); // 일부 테스트에서 미사용 → lenient 처리

        // NotificationType도 @NoArgsConstructor(PROTECTED) 구조이므로 mock 사용
        // type.getTypeName()은 일부 테스트에서만 호출되므로 lenient 처리
        NotificationType type = mock(NotificationType.class);
        lenient().when(type.getTypeName()).thenReturn(NotificationTypeName.CHALLENGE_EXTENSION);

        event = NotificationEvent.builder()
                .type(type)
                .category(NotificationCategory.CHALLENGE)
                .targetType(ResourceType.CHALLENGE)
                .targetId(10L)
                .contextType(ResourceType.ROUND)
                .contextId(20L)
                .title("챌린지 종료 3일 전입니다.")
                .message("다음 라운드에도 참여하시겠어요?")
                .imageKey("challenge/thumbnail.jpg")
                .build();

        delivery = NotificationDelivery.builder()
                .event(event)
                .receiver(user)
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("활성 토큰이 있고 알림 설정이 켜진 경우 FCM 멀티캐스트 발송에 성공한다")
    void sendPushForDeliveries_success() throws FirebaseMessagingException {
        // given
        // NotificationSetting은 @NoArgsConstructor(PROTECTED)라 mock 불가 → 빌더로 실제 객체 생성
        // 기본값이 모두 true이므로 별도 설정 없이 챌린지 활성 상태
        NotificationSetting setting = settingEnabled();
        given(notificationSettingRepository.findByUser(user)).willReturn(Optional.of(setting));
        given(fcmTokenRepository.findAllActiveTokensByUser(user)).willReturn(List.of("token-abc", "token-def"));

        // SendResponse.isSuccessful()이 final이라 mock 불가 → getResponses()를 빈 리스트로 반환해 루프 통과
        BatchResponse batchResponse = mock(BatchResponse.class);
        given(batchResponse.getSuccessCount()).willReturn(2);
        given(batchResponse.getFailureCount()).willReturn(0);
        given(batchResponse.getResponses()).willReturn(List.of());

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, times(1)).sendEachForMulticast(any(MulticastMessage.class));
        }
    }

    @Test
    @DisplayName("챌린지 알림 설정이 꺼진 경우 FCM 발송을 건너뛴다")
    void sendPushForDeliveries_skipsWhenCategoryDisabled() throws FirebaseMessagingException {
        // given
        // 챌린지만 false로 설정한 실제 객체 생성
        NotificationSetting setting = NotificationSetting.builder()
                .isChallengeEnabled(false)
                .isVerificationEnabled(true)
                .isFollowEnabled(true)
                .isBadgeEnabled(true)
                .build();
        given(notificationSettingRepository.findByUser(user)).willReturn(Optional.of(setting));

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, never()).sendEachForMulticast(any());
            verify(fcmTokenRepository, never()).findAllActiveTokensByUser(any());
        }
    }

    @Test
    @DisplayName("전체 알림이 일시 중단된 경우 FCM 발송을 건너뛴다")
    void sendPushForDeliveries_skipsWhenAllPaused() throws FirebaseMessagingException {
        // given
        // 모든 설정을 false로 → isAllPaused() = true
        NotificationSetting setting = NotificationSetting.builder()
                .isChallengeEnabled(false)
                .isVerificationEnabled(false)
                .isFollowEnabled(false)
                .isBadgeEnabled(false)
                .build();
        given(notificationSettingRepository.findByUser(user)).willReturn(Optional.of(setting));

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, never()).sendEachForMulticast(any());
        }
    }

    @Test
    @DisplayName("알림 설정이 없는 경우 기본값으로 발송을 허용한다")
    void sendPushForDeliveries_allowsWhenSettingNotFound() throws FirebaseMessagingException {
        // given
        given(notificationSettingRepository.findByUser(user)).willReturn(Optional.empty());
        given(fcmTokenRepository.findAllActiveTokensByUser(user)).willReturn(List.of("token-abc"));

        BatchResponse batchResponse = mock(BatchResponse.class);
        given(batchResponse.getSuccessCount()).willReturn(1);
        given(batchResponse.getFailureCount()).willReturn(0);
        given(batchResponse.getResponses()).willReturn(List.of());

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, times(1)).sendEachForMulticast(any(MulticastMessage.class));
        }
    }

    @Test
    @DisplayName("활성 FCM 토큰이 없는 경우 발송을 건너뛴다")
    void sendPushForDeliveries_skipsWhenNoActiveTokens() throws FirebaseMessagingException {
        // given
        given(notificationSettingRepository.findByUser(user)).willReturn(Optional.of(settingEnabled()));
        given(fcmTokenRepository.findAllActiveTokensByUser(user)).willReturn(List.of());

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(firebaseMessaging, never()).sendEachForMulticast(any());
        }
    }

    @Test
    @DisplayName("UNREGISTERED 토큰 발송 실패 시 해당 토큰을 비활성화한다")
    void sendPushForDeliveries_deactivatesUnregisteredToken() throws FirebaseMessagingException {
        // given
        String invalidToken = "invalid-token-xyz";

        given(notificationSettingRepository.findByUser(user)).willReturn(Optional.of(settingEnabled()));
        given(fcmTokenRepository.findAllActiveTokensByUser(user)).willReturn(List.of(invalidToken));

        // SendResponse.isSuccessful()이 final이라 mock 불가
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        given(exception.getMessagingErrorCode()).willReturn(MessagingErrorCode.UNREGISTERED);

        SendResponse failResponse = mock(SendResponse.class);
        lenient().when(failResponse.isSuccessful()).thenReturn(false);
        given(failResponse.getException()).willReturn(exception);

        BatchResponse batchResponse = mock(BatchResponse.class);
        given(batchResponse.getSuccessCount()).willReturn(0);
        given(batchResponse.getFailureCount()).willReturn(1);
        given(batchResponse.getResponses()).willReturn(List.of(failResponse));

        FcmToken fcmTokenEntity = mock(FcmToken.class);
        given(fcmTokenRepository.findByToken(invalidToken)).willReturn(Optional.of(fcmTokenEntity));

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDeliveries(List.of(delivery), event);

            // then
            verify(fcmTokenEntity, times(1)).deactivateToken();
            verify(fcmTokenRepository, times(1)).save(fcmTokenEntity);
        }
    }

    @Test
    @DisplayName("sendPushForDelivery는 단일 delivery를 sendPushForDeliveries에 위임한다")
    void sendPushForDelivery_delegatesToSendPushForDeliveries() throws FirebaseMessagingException {
        // given
        given(notificationSettingRepository.findByUser(user)).willReturn(Optional.of(settingEnabled()));
        given(fcmTokenRepository.findAllActiveTokensByUser(user)).willReturn(List.of("token-single"));

        BatchResponse batchResponse = mock(BatchResponse.class);
        given(batchResponse.getSuccessCount()).willReturn(1);
        given(batchResponse.getFailureCount()).willReturn(0);
        given(batchResponse.getResponses()).willReturn(List.of());

        try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
            FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
            fm.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessaging);
            given(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).willReturn(batchResponse);

            // when
            fcmPushService.sendPushForDelivery(delivery, event);

            // then
            verify(firebaseMessaging, times(1)).sendEachForMulticast(any(MulticastMessage.class));
        }
    }


    /**
     * 챌린지 알림이 활성화된 NotificationSetting 실제 객체 생성
     * @NoArgsConstructor(PROTECTED) 때문에 mock() 불가 → @Builder로 생성
     * 기본값이 모두 true이므로 isAllPaused() = false, isChallengeEnabled() = true
     */
    private NotificationSetting settingEnabled() {
        return NotificationSetting.builder()
                .isChallengeEnabled(true)
                .isVerificationEnabled(true)
                .isFollowEnabled(true)
                .isBadgeEnabled(true)
                .build();
    }
}