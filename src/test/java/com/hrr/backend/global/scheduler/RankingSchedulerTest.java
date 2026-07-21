package com.hrr.backend.global.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrr.backend.domain.ranking.repository.UserRankSnapshotRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class RankingSchedulerTest {

    @InjectMocks
    private RankingScheduler rankingScheduler;

    @Mock
    private UserRankSnapshotRepository userRankSnapshotRepository;

    @Test
    @DisplayName("takeWeeklyRankSnapshot: 한국시간 기준 오늘 날짜로 스냅샷 upsert를 호출한다")
    void takeWeeklyRankSnapshot_callsUpsertWithTodayKstDate() {
        // given
        LocalDate expectedDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
        when(userRankSnapshotRepository.upsertWeeklySnapshot(any())).thenReturn(100);

        // when
        rankingScheduler.takeWeeklyRankSnapshot();

        // then
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(userRankSnapshotRepository, times(1)).upsertWeeklySnapshot(dateCaptor.capture());

        assertThat(dateCaptor.getValue()).isEqualTo(expectedDate);
    }
}