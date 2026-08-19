package com.hrr.backend.global.scheduler;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrr.backend.domain.ranking.service.RankingService;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.ranking.repository.UserRankSnapshotRepository;

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
            int created = rankingService.ensureWeeklySnapshot(mondayOfThisWeek);

            //  created > 0 : 방금 내가 만들었음
            //  created == 0: 이미 존재함(다른 인스턴스가 만들었거나 이전 주기에 만들어짐)
            lastConfirmedSnapshotDate = mondayOfThisWeek;


            if (created > 0) {
                log.info("[takeWeeklyRankSnapshot] 주간 랭킹 snapshot 생성을 완료했습니다. snapshotDate={}, createdCount={}",
                        mondayOfThisWeek, created);
            }

        } catch (DataIntegrityViolationException e) {
            // 인스턴스 간 경합 처리 (분산 락 대체)
            lastConfirmedSnapshotDate = mondayOfThisWeek;

            log.info("[takeWeeklyRankSnapshot] 다른 인스턴스가 먼저 snapshot을 생성하여 건너뜁니다. snapshotDate={}",
                    mondayOfThisWeek);

        } catch (Exception e) {
            // 실패해도 스케줄러 밖으로 예외를 던지지 않는다.
            //  이 경로에서는 lastConfirmedSnapshotDate를 갱신하지 않는다. 스냅샷이 확보되지 않았으므로 30초 뒤에 다시 시도되어야 한다.
            log.error("[takeWeeklyRankSnapshot] 주간 랭킹 snapshot 생성에 실패했습니다. 다음 주기에 재시도합니다. snapshotDate={}",
                    mondayOfThisWeek, e);
        }
    }
}