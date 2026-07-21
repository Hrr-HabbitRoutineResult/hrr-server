package com.hrr.backend.global.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.ranking.repository.UserRankSnapshotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RankingScheduler {

    private final UserRankSnapshotRepository userRankSnapshotRepository;

    // 매주 월요일 00:00(한국시간)에 전체 유저 랭킹 스냅샷 생성
    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void takeWeeklyRankSnapshot() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        log.info("[RankingScheduler] 주간 랭킹 스냅샷 생성 시작. snapshotDate={}", today);

        int affected = userRankSnapshotRepository.upsertWeeklySnapshot(today);

        log.info("[RankingScheduler] 주간 랭킹 스냅샷 생성 완료. snapshotDate={}, 총 {}건", today, affected);
    }
}