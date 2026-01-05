package com.hrr.backend.global.scheduler;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrr.backend.domain.round.service.RoundDropService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoundDropScheduler {

    private final RoundDropService roundDropService;

    // 매일 23:59(한국시간)에 "오늘 endDate인 라운드"에서 STOP/UNDECIDED 드랍 처리
    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Seoul")
    public void dropNonContinuers() {
        LocalDate today = LocalDate.now();
        log.info("[RoundDropScheduler] 드랍 처리 시작. endDate={}", today);
        roundDropService.dropNonContinuersAt(today);
    }
}
