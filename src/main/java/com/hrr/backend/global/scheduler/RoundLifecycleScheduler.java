package com.hrr.backend.global.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrr.backend.domain.round.service.RoundLifecycleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoundLifecycleScheduler {

    private final RoundLifecycleService roundLifecycleService;

    // 매일 00:10에 “어제 종료된 라운드” 처리
    @Scheduled(cron = "0 10 0 * * *")
    public void closeEndedRounds() {
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        log.info("[RoundLifecycleScheduler] 종료 라운드 처리 시작. endDate={}", yesterday);
        roundLifecycleService.processRoundsEndedAt(yesterday);
    }
}
