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

        // 같은 날짜로 재실행되는 경우(예: 배치 재처리)를 대비해, 기존 행을 먼저 지우고 다시 적재한다.
        // (ACTIVE 집합에서 빠진 유저의 예전 행이 그대로 남아 total_user_count/노출 순위가 어긋나는 것을 방지)
        int deleted = userRankSnapshotRepository.deleteBySnapshotDate(today);
        log.info("[RankingScheduler] 기존 스냅샷 행 정리. snapshotDate={}, 삭제 {}건", today, deleted);

        int affected = userRankSnapshotRepository.upsertWeeklySnapshot(today);

        log.info("[RankingScheduler] 주간 랭킹 스냅샷 생성 완료. snapshotDate={}, 총 {}건", today, affected);
    }
}