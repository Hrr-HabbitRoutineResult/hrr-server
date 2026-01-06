package com.hrr.backend.global.scheduler;

import com.hrr.backend.domain.notification.event.ChallengeExtensionResponseEvent;
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
    private RoundRepository roundRepository;

    @Mock
    private RoundRecordRepository roundRecordRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    @DisplayName("결과 알림 스케줄러: 대상 라운드의 참여자 전원에게 이벤트를 발행한다")
    void scheduleChallengeExtensionResultNotifications_Success() {
        // given
        // 결정 기간 종료 후(endDate - 1) 시점을 테스트하기 위해 대상 날짜 설정
        LocalDate targetEndDate = LocalDate.now().plusDays(1);
        Round round = mock(Round.class);
        given(round.getId()).willReturn(100L);

        // 종료 예정 라운드 조회 모킹
        given(roundRepository.findAllByEndDate(targetEndDate)).willReturn(List.of(round));

        User user = User.builder().id(1L).build();
        RoundRecord record = mock(RoundRecord.class);
        UserChallenge uc = mock(UserChallenge.class);

        given(record.getUserChallenge()).willReturn(uc);
        given(uc.getUser()).willReturn(user);
        given(record.getNextRoundIntent()).willReturn(NextRoundIntent.CONTINUE);

        // 해당 라운드 참여자 목록 조회 모킹
        given(roundRecordRepository.findAllByRoundWithUserAndSetting(round, ChallengeJoinStatus.JOINED))
                .willReturn(List.of(record));

        // when
        notificationScheduler.scheduleChallengeExtensionResultNotifications();

        // then
        // 참여자 수(1명)만큼 이벤트가 발행되었는지 검증
        verify(eventPublisher, times(1)).publishEvent(any(ChallengeExtensionResponseEvent.class));
    }

    @Test
    @DisplayName("결과 알림 스케줄러: 대상 라운드가 없으면 이벤트를 발행하지 않는다")
    void scheduleChallengeExtensionResultNotifications_NoRounds() {
        // given
        LocalDate targetEndDate = LocalDate.now().plusDays(1);
        given(roundRepository.findAllByEndDate(targetEndDate)).willReturn(List.of());

        // when
        notificationScheduler.scheduleChallengeExtensionResultNotifications();

        // then
        // 라운드가 없으므로 참여자 조회나 이벤트 발행이 일어나지 않아야 함
        verify(roundRecordRepository, never()).findAllByRoundWithUserAndSetting(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }
}