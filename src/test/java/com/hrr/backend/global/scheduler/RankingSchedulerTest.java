package com.hrr.backend.global.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.hrr.backend.domain.ranking.service.RankingService;

@ExtendWith(MockitoExtension.class)
class RankingSchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Mock
    private RankingService rankingService;

    // 실행 시점의 실제 시각 대신, 테스트마다 원하는 날짜/시각으로 고정된 Clock을 주입해서 검증한다.
    // (실제 시각을 그대로 읽으면 테스트 실행 요일에 따라 결과가 달라진다)
    private Clock fixedClockAt(LocalDateTime dateTime) {
        return Clock.fixed(dateTime.atZone(KST).toInstant(), KST);
    }

    @Test
    @DisplayName("takeWeeklyRankSnapshot: 화요일에 실행돼도 그 주의 월요일 날짜로 스냅샷 생성을 요청한다")
    void takeWeeklyRankSnapshot_usesMondayOfCurrentWeek_whenRunOnOtherDay() {
        // given: 2026-08-18(화) 09:30에 실행된 상황
        Clock fixedClock = fixedClockAt(LocalDateTime.of(2026, 8, 18, 9, 30));
        RankingScheduler rankingScheduler = new RankingScheduler(rankingService, fixedClock);

        LocalDate expectedMonday = LocalDate.of(2026, 8, 17);
        when(rankingService.ensureWeeklySnapshot(expectedMonday)).thenReturn(120);

        // when
        rankingScheduler.takeWeeklyRankSnapshot();

        // then: 오늘(08-18)이 아니라 그 주의 월요일(08-17)로 호출되어야 함
        verify(rankingService, times(1)).ensureWeeklySnapshot(expectedMonday);
    }

    @Test
    @DisplayName("takeWeeklyRankSnapshot: 월요일 00:00에 실행되면 오늘 날짜가 그대로 스냅샷 기준일이 된다")
    void takeWeeklyRankSnapshot_usesToday_whenRunOnMonday() {
        // given: 2026-08-24(월) 00:00:00에 실행된 상황
        Clock fixedClock = fixedClockAt(LocalDateTime.of(2026, 8, 24, 0, 0));
        RankingScheduler rankingScheduler = new RankingScheduler(rankingService, fixedClock);

        LocalDate expectedMonday = LocalDate.of(2026, 8, 24);
        when(rankingService.ensureWeeklySnapshot(expectedMonday)).thenReturn(120);

        // when
        rankingScheduler.takeWeeklyRankSnapshot();

        // then: previousOrSame(MONDAY)이므로 월요일 당일에는 오늘 날짜 그대로
        verify(rankingService, times(1)).ensureWeeklySnapshot(expectedMonday);
    }

    @Test
    @DisplayName("takeWeeklyRankSnapshot: 이번 주 스냅샷이 확보된 뒤에는 다시 호출해도 DB를 조회하지 않는다")
    void takeWeeklyRankSnapshot_doesNotHitServiceAgain_afterSnapshotConfirmed() {
        // given
        Clock fixedClock = fixedClockAt(LocalDateTime.of(2026, 8, 18, 9, 30));
        RankingScheduler rankingScheduler = new RankingScheduler(rankingService, fixedClock);

        LocalDate expectedMonday = LocalDate.of(2026, 8, 17);
        when(rankingService.ensureWeeklySnapshot(expectedMonday)).thenReturn(120);

        // when: 30초 주기로 세 번 깨어난 상황을 가정
        rankingScheduler.takeWeeklyRankSnapshot();
        rankingScheduler.takeWeeklyRankSnapshot();
        rankingScheduler.takeWeeklyRankSnapshot();

        // then: 첫 호출에서 확보되었으므로 서비스는 단 한 번만 호출되어야 한다
        verify(rankingService, times(1)).ensureWeeklySnapshot(expectedMonday);
    }

    @Test
    @DisplayName("takeWeeklyRankSnapshot: 생성에 실패하면 예외를 삼키고 다음 주기에 다시 시도한다")
    void takeWeeklyRankSnapshot_retriesNextCycle_whenSnapshotCreationFails() {
        // given: DB 오류 등으로 생성이 실패하는 상황
        Clock fixedClock = fixedClockAt(LocalDateTime.of(2026, 8, 18, 9, 30));
        RankingScheduler rankingScheduler = new RankingScheduler(rankingService, fixedClock);

        LocalDate expectedMonday = LocalDate.of(2026, 8, 17);
        when(rankingService.ensureWeeklySnapshot(expectedMonday))
                .thenThrow(new IllegalStateException("DB 연결 실패"));

        // when & then: 예외가 스케줄러 밖으로 새어 나가면 안 된다
        assertThatCode(rankingScheduler::takeWeeklyRankSnapshot).doesNotThrowAnyException();
        assertThatCode(rankingScheduler::takeWeeklyRankSnapshot).doesNotThrowAnyException();

        // 실패한 경우에는 확보 처리를 하지 않으므로, 다음 주기에도 다시 시도되어야 한다
        verify(rankingService, times(2)).ensureWeeklySnapshot(expectedMonday);
    }

    @Test
    @DisplayName("takeWeeklyRankSnapshot: 다른 인스턴스가 먼저 생성해 유니크 제약에 걸려도 정상 종료하고 재시도하지 않는다")
    void takeWeeklyRankSnapshot_stopsRetrying_whenAnotherInstanceCreatedSnapshot() {
        // given: 블루/그린 두 인스턴스가 동시에 INSERT를 시도해 늦은 쪽이 롤백된 상황
        Clock fixedClock = fixedClockAt(LocalDateTime.of(2026, 8, 18, 9, 30));
        RankingScheduler rankingScheduler = new RankingScheduler(rankingService, fixedClock);

        LocalDate expectedMonday = LocalDate.of(2026, 8, 17);
        when(rankingService.ensureWeeklySnapshot(expectedMonday))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        // when
        assertThatCode(rankingScheduler::takeWeeklyRankSnapshot).doesNotThrowAnyException();
        rankingScheduler.takeWeeklyRankSnapshot();

        // then: 이미 스냅샷이 존재하는 상태이므로 두 번째 주기에는 서비스를 호출하지 않는다
        verify(rankingService, times(1)).ensureWeeklySnapshot(expectedMonday);
    }
}