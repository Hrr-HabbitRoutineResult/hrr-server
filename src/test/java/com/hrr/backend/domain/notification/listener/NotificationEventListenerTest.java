package com.hrr.backend.domain.notification.listener;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.notification.entity.NotificationType;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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

    @Test
    @DisplayName("알림 설정 여부와 상관없이 모든 참여자에게 알림 목록 데이터가 생성되어야 한다")
    void handleChallengeExtensionEvent_Success() {
        // given
        Long roundId = 1L;
        String testImageKey = "challenges/thumbnail_01.png"; // 테스트용 키
        ChallengeExtensionEvent event = new ChallengeExtensionEvent(roundId);

        // 1. 멱등성 체크 통과
        given(eventRepository.existsByContextTypeAndContextIdAndCreatedAtAfter(any(), any(), any()))
                .willReturn(false);

        // 2. 라운드 및 챌린지 모킹 (imageKey 포함)
        Round round = mock(Round.class);
        Challenge challenge = Challenge.builder()
                .id(10L)
                .title("테스트 챌린지")
                .imageKey(testImageKey) // 이미지 키 설정
                .build();
        given(roundRepository.findById(roundId)).willReturn(Optional.of(round));
        given(round.getChallenge()).willReturn(challenge);

        // 3. 알림 타입 설정 (Enum 직접 사용)
        NotificationType type = NotificationType.builder()
                .typeName(NotificationTypeName.CHALLENGE_EXTENSION)
                .build();
        given(typeRepository.findByTypeName(NotificationTypeName.CHALLENGE_EXTENSION))
                .willReturn(Optional.of(type));

        // 4. 참여자 설정 (설정 ON/OFF 유저 각 1명)
        User userEnabled = User.builder().id(1L).notificationSetting(
                NotificationSetting.builder().isChallengeEnabled(true).build()).build();
        User userDisabled = User.builder().id(2L).notificationSetting(
                NotificationSetting.builder().isChallengeEnabled(false).build()).build();

        RoundRecord record1 = mock(RoundRecord.class);
        RoundRecord record2 = mock(RoundRecord.class);
        UserChallenge uc1 = mock(UserChallenge.class);
        UserChallenge uc2 = mock(UserChallenge.class);

        given(record1.getUserChallenge()).willReturn(uc1);
        given(record2.getUserChallenge()).willReturn(uc2);
        given(uc1.getUser()).willReturn(userEnabled);
        given(uc2.getUser()).willReturn(userDisabled);

        given(roundRecordRepository.findAllByRoundWithUserAndSetting(
                round,
                ChallengeJoinStatus.JOINED
        ))
                .willReturn(List.of(record1, record2));

        // when
        notificationEventListener.handleChallengeExtensionEvent(event);

        // then
        // 1. NotificationEvent 저장 시 imageKey가 정확히 포함되었는지 확인
        verify(eventRepository).save(argThat(savedEvent ->
                savedEvent.getImageKey().equals(testImageKey) &&
                        savedEvent.getTitle().contains("테스트 챌린지")
        ));

        // 중요: 설정과 관계없이 참여자 전원(2명)에게 알림 데이터가 저장되어야 함
        verify(notificationRepository).saveAll(argThat(deliveries -> ((List<?>)deliveries).size() == 2));
    }

    @Test
    @DisplayName("오늘 이미 알림이 생성되었다면 로직을 수행하지 않는다")
    void handleChallengeExtensionEvent_Idempotency() {
        // given
        Long roundId = 1L;
        ChallengeExtensionEvent event = new ChallengeExtensionEvent(roundId);

        given(eventRepository.existsByContextTypeAndContextIdAndCreatedAtAfter(
                eq(ResourceType.ROUND), eq(roundId), any()))
                .willReturn(true);

        // when
        notificationEventListener.handleChallengeExtensionEvent(event);

        // then
        verify(roundRepository, never()).findById(anyLong());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("챌린지 연장 '계속하기' 응답 시 성공 알림이 생성된다")
    void handleChallengeExtensionResponse_Continue() {
        // given
        User user = createTestUser(1L, "테스터"); //
        Challenge challenge = createTestChallenge("운동 챌린지");
        Round round = createTestRound(10L, challenge);

        NotificationType successType = NotificationType.builder()
                .typeName(NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS)
                .build();

        ChallengeExtensionResponseEvent event = new ChallengeExtensionResponseEvent(
                round.getId(), user, NextRoundIntent.CONTINUE);

        given(roundRepository.findById(anyLong())).willReturn(Optional.of(round));
        given(typeRepository.findByTypeName(NotificationTypeName.CHALLENGE_EXTENSION_SUCCESS))
                .willReturn(Optional.of(successType));

        // when
        notificationEventListener.handleChallengeExtensionResponseEvent(event);

        // then
        // 1. NotificationEvent 저장 확인
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventRepository).save(eventCaptor.capture());

        NotificationEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getMessage()).contains("다음 라운드에서도 루틴을 이어가요");
        assertThat(savedEvent.getImageKey()).isEqualTo(challenge.getImageKey());

        // 2. NotificationDelivery 저장 확인 (수신자 검증)
        verify(notificationRepository).save(any(NotificationDelivery.class));
    }

    @Test
    @DisplayName("챌린지 연장 '그만하기' 응답 시 종료 알림이 생성된다")
    void handleChallengeExtensionResponse_Stop() {
        // given
        User user = createTestUser(1L, "테스터");
        Challenge challenge = createTestChallenge("운동 챌린지");
        Round round = createTestRound(10L, challenge);

        NotificationType cancelType = NotificationType.builder()
                .typeName(NotificationTypeName.CHALLENGE_EXTENSION_CANCEL)
                .build();

        ChallengeExtensionResponseEvent event = new ChallengeExtensionResponseEvent(
                round.getId(), user, NextRoundIntent.STOP);

        given(roundRepository.findById(anyLong())).willReturn(Optional.of(round));
        given(typeRepository.findByTypeName(NotificationTypeName.CHALLENGE_EXTENSION_CANCEL))
                .willReturn(Optional.of(cancelType));

        // when
        notificationEventListener.handleChallengeExtensionResponseEvent(event);

        // then
        ArgumentCaptor<NotificationEvent> eventCaptor = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventRepository).save(eventCaptor.capture());

        NotificationEvent savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getMessage()).contains("챌린지가 예정대로 종료돼요. 그동안 수고 많으셨어요");
        assertThat(savedEvent.getTargetId()).isEqualTo(user.getId());
    }

    // --- Helper Methods ---
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