package com.hrr.backend.global.scheduler;

import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeSchedulerTest {

    @InjectMocks
    private ChallengeScheduler challengeScheduler;

    @Mock
    private ChallengeRepository challengeRepository;

    @Test
    @DisplayName("updateChallengeStatus: 시작 대상 챌린지가 있으면 findIdsToStart 후 bulk UPDATE를 수행한다")
    void updateChallengeStatus_UpdatesDB_WhenTargetsExist() {
        // Given
        LocalDate today = LocalDate.now();
        LocalDateTime expectedReferenceTime = today.atStartOfDay();

        // findIdsToStart가 대상 ID들을 리턴하도록 세팅
        when(challengeRepository.findIdsToStart(
                eq(ChallengeStatus.UPCOMING),
                any(LocalDateTime.class))
        ).thenReturn(List.of(1L, 2L));

        when(challengeRepository.updateChallengeStatusToOngoing(
                any(), any(), any())
        ).thenReturn(2);

        // When
        challengeScheduler.updateChallengeStatus();

        // Then
        ArgumentCaptor<LocalDateTime> referenceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        // findIdsToStart 호출 시점의 기준 시각 검증
        verify(challengeRepository).findIdsToStart(
                eq(ChallengeStatus.UPCOMING),
                referenceCaptor.capture()
        );

        assertThat(referenceCaptor.getValue()).isEqualTo(expectedReferenceTime);

        // updateChallengeStatusToOngoing도 동일한 기준 시각으로 호출되는지 검증
        verify(challengeRepository).updateChallengeStatusToOngoing(
                eq(ChallengeStatus.ONGOING),
                eq(ChallengeStatus.UPCOMING),
                eq(expectedReferenceTime)
        );
    }

    @Test
    @DisplayName("updateChallengeStatus: 시작 대상 챌린지가 없으면 bulk UPDATE는 호출되지 않는다")
    void updateChallengeStatus_DoesNothing_WhenNoTargets() {
        // Given
        when(challengeRepository.findIdsToStart(
                eq(ChallengeStatus.UPCOMING),
                any(LocalDateTime.class))
        ).thenReturn(List.of());

        // When
        challengeScheduler.updateChallengeStatus();

        // Then
        verify(challengeRepository).findIdsToStart(
                eq(ChallengeStatus.UPCOMING),
                any(LocalDateTime.class)
        );

        // UPDATE 쿼리는 호출되면 안 됨
        verify(challengeRepository, never()).updateChallengeStatusToOngoing(
                any(), any(), any()
        );
    }
}
