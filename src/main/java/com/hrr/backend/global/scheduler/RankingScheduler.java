package com.hrr.backend.global.scheduler;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrr.backend.domain.ranking.service.RankingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankingScheduler {

    private static final int FAILURE_ALERT_THRESHOLD = 10;
    private static final int FAILURE_ALERT_INTERVAL = 120;

    private final RankingService rankingService;
    private final Clock clock;

    private volatile LocalDate lastConfirmedSnapshotDate;

    private final AtomicInteger consecutiveFailures = new AtomicInteger();

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

                // 복구 로그

                int recoveredFailureCount = consecutiveFailures.getAndSet(0);
                if (recoveredFailureCount > 0) {
                    log.info("[takeWeeklyRankSnapshot] 주간 랭킹 snapshot 생성이 복구되었습니다. snapshotDate={}, previousFailureCount={}",
                            mondayOfThisWeek, recoveredFailureCount);
                }
                return;
            }

            // 예외 없이 스냅샷이 확보되지 않은 경우

            handleFailure(mondayOfThisWeek, "대상 ACTIVE User가 없어 주간 랭킹 snapshot이 생성되지 않았습니다.", null);

        } catch (Exception e) {
            // log.error -> handleFailure()로 위임
            handleFailure(mondayOfThisWeek, "주간 랭킹 snapshot 생성에 실패했습니다.", e);
        }
    }

    // 실패 처리 및 로깅 (로깅 컨벤션 1번 / 5번 반영)
    private void handleFailure(LocalDate snapshotDate, String reason, Exception e) {
        int failureCount = consecutiveFailures.incrementAndGet();

        log.warn("[takeWeeklyRankSnapshot] {} 다음 주기에 재시도합니다. snapshotDate={}, exception={}",
                reason, snapshotDate, e == null ? "none" : e.getClass().getSimpleName());

        if (failureCount == FAILURE_ALERT_THRESHOLD || failureCount % FAILURE_ALERT_INTERVAL == 0) {
            log.error("[takeWeeklyRankSnapshot] 주간 랭킹 snapshot 생성의 연속 실패가 누적되었습니다. snapshotDate={}, consecutiveFailureCount={}",
                    snapshotDate, failureCount, e);
        }
    }
}