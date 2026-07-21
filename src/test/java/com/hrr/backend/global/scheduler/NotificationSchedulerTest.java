package com.hrr.backend.global.scheduler;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.notification.event.ChallengeStartEvent;
import com.hrr.backend.domain.notification.service.NotificationCommandService;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @InjectMocks
    private NotificationScheduler notificationScheduler;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private RoundRecordRepository roundRecordRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationCommandService notificationCommandService;

    @Test
    @DisplayName("챌린지 시작 알림 스케줄러: 내일 시작하는 챌린지마다 이벤트를 발행한다")
    void scheduleChallengeStartNotifications_Success() {
        // given
        LocalDate targetDate = LocalDate.now().plusDays(1);
        Challenge challenge = mock(Challenge.class);
        given(challenge.getId()).willReturn(10L);
        given(challengeRepository.findAllByStartDateGreaterThanEqualAndStartDateLessThan(
                targetDate.atStartOfDay(),
                targetDate.plusDays(1).atStartOfDay()
        )).willReturn(List.of(challenge));

        // when
        notificationScheduler.scheduleChallengeStartNotifications();

        // then
        verify(eventPublisher, times(1)).publishEvent(any(ChallengeStartEvent.class));
    }

    @Test
    @DisplayName("챌린지 시작 알림 스케줄러: 내일 시작하는 챌린지가 없으면 이벤트를 발행하지 않는다")
    void scheduleChallengeStartNotifications_NoChallenges() {
        // given
        LocalDate targetDate = LocalDate.now().plusDays(1);
        given(challengeRepository.findAllByStartDateGreaterThanEqualAndStartDateLessThan(
                targetDate.atStartOfDay(),
                targetDate.plusDays(1).atStartOfDay()
        )).willReturn(List.of());

        // when
        notificationScheduler.scheduleChallengeStartNotifications();

        // then
        verify(eventPublisher, never()).publishEvent(any());
    }

}
