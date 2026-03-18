package com.hrr.backend.domain.notification.listener;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.fcm.event.FcmPushSendEvent; // 추가된 이벤트
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.notification.entity.NotificationType;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.entity.enums.ResourceType;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionResponseEvent;
import com.hrr.backend.domain.notification.repository.NotificationEventRepository;
import com.hrr.backend.domain.notification.repository.NotificationRepository;
import com.hrr.backend.domain.notification.repository.NotificationTypeRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher; // 추가

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @InjectMocks
    private NotificationEventListener notificationEventListener;

    @Mock private RoundRepository roundRepository;
    @Mock private RoundRecordRepository roundRecordRepository;
    @Mock private NotificationTypeRepository typeRepository;
    @Mock private NotificationEventRepository eventRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private ApplicationEventPublisher eventPublisher; // FcmPushService 대신 추가

    @Test
    @DisplayName("알림 설정 여부와 상관없이 모든 참여자에게 데이터가 저장되고 FCM 발송 이벤트가 발행된다")
    void handleChallengeExtensionEvent_Success() {
        // given
        Long roundId = 1L;
        String testImageKey = "challenges/thumbnail_01.png";
        ChallengeExtensionEvent event = new ChallengeExtensionEvent(roundId);

        given(eventRepository.existsByContextTypeAndContextIdAndCreatedAtAfter(any(), any(), any()))
                .willReturn(false);

        Round round = mock(Round.class);
        Challenge challenge = Challenge.builder()
                .id(10L)
                .title("테스트 챌린지")
                .imageKey(testImageKey)
                .build();
        given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
        given(round.getChallenge()).willReturn(challenge);

        NotificationType type = NotificationType.builder()
                .typeName(NotificationTypeName.CHALLENGE_EXTENSION)
                .build();
        given(typeRepository.findByTypeName(NotificationTypeName.CHALLENGE_EXTENSION))
                .willReturn(Optional.of(type));

        User user1 = User.builder().id(1L).nickname("유저1").build();
        User user2 = User.builder().id(2L).nickname("유저2").build();

        RoundRecord record1 = mock(RoundRecord.class);
        RoundRecord record2 = mock(RoundRecord.class);
        UserChallenge uc1 = mock(UserChallenge.class);
        UserChallenge uc2 = mock(UserChallenge.class);

        given(record1.getUserChallenge()).willReturn(uc1);
        given(record2.getUserChallenge()).willReturn(uc2);
        given(uc1.getUser()).willReturn(user1);
        given(uc2.getUser()).willReturn(user2);

        given(roundRecordRepository.findAllByRoundWithUserAndSetting(round, ChallengeJoinStatus.JOINED))
                .willReturn(List.of(record1, record2));

        // when
        notificationEventListener.handleChallengeExtensionEvent(event);

        // then
        // DB 저장 확인
        verify(notificationRepository).saveAll(anyList());

        // FCM 발송 이벤트 발행 확인
        ArgumentCaptor<FcmPushSendEvent> pushEventCaptor = ArgumentCaptor.forClass(FcmPushSendEvent.class);
        verify(eventPublisher).publishEvent(pushEventCaptor.capture());

        FcmPushSendEvent publishedEvent = pushEventCaptor.getValue();
        assertThat(publishedEvent.deliveries()).hasSize(2);
        assertThat(publishedEvent.notificationEvent().getTitle()).contains("테스트 챌린지");
    }

    @Test
    @DisplayName("챌린지 연장 응답 시 알림 데이터가 저장되고 FCM 발송 이벤트가 발행된다")
    void handleChallengeExtensionResponse_Success() {
        // given
        User user = createTestUser(1L, "테스터");
        Challenge challenge = createTestChallenge("운동 챌린지");
        Round round = createTestRound(10L, challenge);
        ChallengeExtensionResponseEvent event = new ChallengeExtensionResponseEvent(round.getId(), user, NextRoundIntent.CONTINUE);

        NotificationType successType = NotificationType.builder().typeName(NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS).build();

        given(roundRepository.findById(round.getId())).willReturn(Optional.of(round));
        given(typeRepository.findByTypeName(NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS)).willReturn(Optional.of(successType));

        // when
        notificationEventListener.handleChallengeExtensionResponseEvent(event);

        // then
        // DB 저장 확인
        verify(notificationRepository).save(any(NotificationDelivery.class));

        // FCM 발송 이벤트 발행 확인
        verify(eventPublisher).publishEvent(any(FcmPushSendEvent.class));
    }

    @Test
    @DisplayName("멱등성 체크에 걸리면 이벤트를 발행하지 않는다")
    void handleChallengeExtensionEvent_Idempotency() {
        // given
        Long roundId = 1L;
        ChallengeExtensionEvent event = new ChallengeExtensionEvent(roundId);

        given(eventRepository.existsByContextTypeAndContextIdAndCreatedAtAfter(any(), any(), any()))
                .willReturn(true);

        // when
        notificationEventListener.handleChallengeExtensionEvent(event);

        // then
        verify(notificationRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publishEvent(any()); // 이벤트가 발행되지 않아야 함
    }

    private User createTestUser(Long id, String nickname) {
        return User.builder().id(id).nickname(nickname).build();
    }

    private Challenge createTestChallenge(String title) {
        return Challenge.builder().id(1L).title(title).imageKey("test-image-key").build();
    }

    private Round createTestRound(Long id, Challenge challenge) {
        return Round.builder().id(id).challenge(challenge).build();
    }
}