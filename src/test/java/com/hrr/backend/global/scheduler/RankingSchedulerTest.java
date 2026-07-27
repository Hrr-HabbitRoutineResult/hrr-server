package com.hrr.backend.global.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrr.backend.domain.ranking.repository.UserRankSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class RankingSchedulerTest {

    @Mock
    private UserRankSnapshotRepository userRankSnapshotRepository;

    // 실행 시점의 실제 시각 대신, 테스트마다 원하는 날짜로 고정된 Clock을 주입해서 검증한다.
    // (실제 시각을 그대로 읽으면 테스트 실행이 자정을 넘는 순간 간헐적으로 결과가 달라질 수 있음)
    @Test
    @DisplayName("takeWeeklyRankSnapshot: 주입된 Clock 기준 날짜로 기존 스냅샷을 먼저 삭제한 뒤 upsert를 호출한다")
    void takeWeeklyRankSnapshot_deletesExistingRowsBeforeUpsert() {
        // given
        LocalDate fixedDate = LocalDate.of(2026, 7, 20); // 임의로 고정한 날짜(월요일)
        Clock fixedClock = Clock.fixed(
                fixedDate.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(),
                ZoneId.of("Asia/Seoul")
        );
        RankingScheduler rankingScheduler = new RankingScheduler(userRankSnapshotRepository, fixedClock);

        when(userRankSnapshotRepository.deleteBySnapshotDate(any())).thenReturn(3);
        when(userRankSnapshotRepository.upsertWeeklySnapshot(any())).thenReturn(100);

        // when
        rankingScheduler.takeWeeklyRankSnapshot();

        // then: 삭제 -> upsert 순서로, 고정된 날짜 그대로 호출되어야 함
        InOrder inOrder = Mockito.inOrder(userRankSnapshotRepository);
        inOrder.verify(userRankSnapshotRepository, times(1)).deleteBySnapshotDate(fixedDate);
        inOrder.verify(userRankSnapshotRepository, times(1)).upsertWeeklySnapshot(fixedDate);
    }
}