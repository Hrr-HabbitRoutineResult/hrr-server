package com.hrr.backend.global.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrr.backend.domain.ranking.repository.UserRankSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class RankingSchedulerTest {

    @InjectMocks
    private RankingScheduler rankingScheduler;

    @Mock
    private UserRankSnapshotRepository userRankSnapshotRepository;

    @Test
    @DisplayName("takeWeeklyRankSnapshot: 한국시간 기준 오늘 날짜로 기존 스냅샷을 먼저 삭제한 뒤 upsert를 호출한다")
    void takeWeeklyRankSnapshot_deletesExistingRowsBeforeUpsert() {
        // given
        LocalDate expectedDate = LocalDate.now(ZoneId.of("Asia/Seoul"));
        when(userRankSnapshotRepository.deleteBySnapshotDate(any())).thenReturn(3);
        when(userRankSnapshotRepository.upsertWeeklySnapshot(any())).thenReturn(100);

        // when
        rankingScheduler.takeWeeklyRankSnapshot();

        // then: 삭제 -> upsert 순서로 호출되어야 함 (같은 트랜잭션 내 원자적 교체)
        InOrder inOrder = Mockito.inOrder(userRankSnapshotRepository);
        inOrder.verify(userRankSnapshotRepository, times(1)).deleteBySnapshotDate(expectedDate);
        inOrder.verify(userRankSnapshotRepository, times(1)).upsertWeeklySnapshot(expectedDate);

        ArgumentCaptor<LocalDate> deleteDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(userRankSnapshotRepository, times(1)).deleteBySnapshotDate(deleteDateCaptor.capture());
        assertThat(deleteDateCaptor.getValue()).isEqualTo(expectedDate);

        ArgumentCaptor<LocalDate> upsertDateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(userRankSnapshotRepository, times(1)).upsertWeeklySnapshot(upsertDateCaptor.capture());
        assertThat(upsertDateCaptor.getValue()).isEqualTo(expectedDate);
    }
}