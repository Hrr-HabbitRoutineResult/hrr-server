package com.hrr.backend.global.scheduler;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // [추가] 로깅 라이브러리
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j // [추가]
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final RoundRepository roundRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
    public void scheduleChallengeExtensionNotifications() {
        // 대상 날짜 계산
        LocalDate targetDate = LocalDate.now().plusDays(Challenge.CHALLENGER_DECISION_DAYS);

        try {
            // 스케줄러 시작 및 대상 날짜 로깅
            log.info("[ChallengeExtensionScheduler] 챌린지 연장 알림 스케줄러 시작 (대상 날짜: {})", targetDate);

            List<Round> targetRounds = roundRepository.findAllByEndDate(targetDate);

            // 발견된 라운드 수 로깅
            log.info("[ChallengeExtensionScheduler] 대상 날짜({})에 종료되는 라운드 총 {}건 발견", targetDate, targetRounds.size());

            if (targetRounds.isEmpty()) {
                log.info("[ChallengeExtensionScheduler] 알림을 보낼 대상 라운드가 없습니다.");
                return;
            }

            for (Round round : targetRounds) {
                try {
                    // 각 라운드별 이벤트 발행 시도 로깅
                    eventPublisher.publishEvent(new ChallengeExtensionEvent(round.getId()));
                    log.info("[ChallengeExtensionScheduler] 이벤트 발행 완료 - RoundId: {}", round.getId());
                } catch (Exception e) {
                    // 개별 라운드 처리 중 예외 발생 시 에러 로깅
                    log.error("[ChallengeExtensionScheduler] 이벤트 발행 실패 - RoundId: {}, 사유: {}", round.getId(), e.getMessage());
                }
            }

            log.info("[ChallengeExtensionScheduler] 스케줄러 작업 정상 종료");

        } catch (Exception e) {
            // 전체 프로세스 중 예외 발생 시 에러 로깅
            log.error("[ChallengeExtensionScheduler] 스케줄러 실행 중 심각한 오류 발생 (대상 날짜: {}), 에러: ", targetDate, e);
        }
    }
}