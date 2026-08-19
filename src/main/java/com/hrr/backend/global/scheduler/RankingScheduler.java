package com.hrr.backend.global.scheduler;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrr.backend.domain.ranking.service.RankingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankingScheduler {

    private final RankingService rankingService;
    private final Clock clock;

    private volatile LocalDate lastConfirmedSnapshotDate;

    /** 이번 주 월요일자 랭킹 스냅샷이 확보될 때까지 30초마다 생성을 시도한다. */
    @Scheduled(cron = "*/30 * * * * *", zone = "Asia/Seoul")
    public void takeWeeklyRankSnapshot() {

        LocalDate mondayOfThisWeek = LocalDate.now(clock)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // 이번 주 스냅샷이 이미 확보되었으면 DB에 가지 않고 즉시 종료
        if (mondayOfThisWeek.equals(lastConfirmedSnapshotDate)) {
            return;
        }

        try {
            boolean confirmed = rankingService.ensureWeeklySnapshot(mondayOfThisWeek);

            if (confirmed) {
                lastConfirmedSnapshotDate = mondayOfThisWeek;
            }

        } catch (Exception e) {
            log.error("[takeWeeklyRankSnapshot] 주간 랭킹 snapshot 생성에 실패했습니다. 다음 주기에 재시도합니다. snapshotDate={}",
                    mondayOfThisWeek, e);
        }
    }
}