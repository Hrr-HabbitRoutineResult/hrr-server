package com.hrr.backend.global.scheduler;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionResponseEvent;
import com.hrr.backend.domain.notification.event.VerificationDeadlineEvent;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.global.common.enums.ChallengeDays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final RoundRepository roundRepository;
    private final RoundRecordRepository roundRecordRepository;
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

    /**
     * 연장 응답 기간 종료 직후(자정) 최종 결과 알림 발송
     * 결정 기간(endDate - 2)이 종료되는 시점인 자정에 바로 실행
     */
    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 실행
    public void scheduleChallengeExtensionResultNotifications() {
        // 오늘(자정)이 endDate - 1일인 라운드들을 찾음
        LocalDate targetEndDate = LocalDate.now().plusDays(1);

        log.info("[ExtensionResultScheduler] 결정 기간 종료. 결과 알림 발송 시작 (대상 endDate: {})", targetEndDate);

        List<Round> targetRounds = roundRepository.findAllByEndDate(targetEndDate);

        for (Round round : targetRounds) {
            try { // 1차 격리: 특정 라운드 조회 실패 시에도 다른 라운드는 진행
                List<RoundRecord> records = roundRecordRepository.findAllByRoundWithUserAndSetting(
                        round, ChallengeJoinStatus.JOINED);

                for (RoundRecord record : records) {
                    try { // 2차 격리: 특정 사용자 이벤트 발행 실패 시에도 다음 사용자는 진행
                        eventPublisher.publishEvent(new ChallengeExtensionResponseEvent(
                                round.getId(),
                                record.getUserChallenge().getUser(),
                                record.getNextRoundIntent()
                        ));
                    } catch (Exception e) {
                        log.error("[ExtensionResultScheduler] 이벤트 발행 실패 - RoundId: {}, UserId: {}, 사유: {}",
                                round.getId(), record.getUserChallenge().getUser().getId(), e.getMessage());
                    }
                }
                log.info("[ExtensionResultScheduler] RoundId: {} 결과 알림 발행 완료", round.getId());
            } catch (Exception e) {
                log.error("[ExtensionResultScheduler] 라운드 처리 실패 - RoundId: {}, 사유: {}", round.getId(), e.getMessage());
            }
        }
    }

    /**
     * 인증 마감 알림 스케줄링 (매일 자정)
     * - 오늘 인증 요일인 진행 중 라운드 조회
     * - VerificationDeadlineEvent를 발행
     */
    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정에 오늘 인증 요일 라운드 스케줄링
    public void scheduleVerificationDeadlineNotifications() {
        LocalDate today = LocalDate.now();
        ChallengeDays todayEnum = ChallengeDays.from(today.getDayOfWeek());

        log.info("[VerificationDeadlineScheduler] 인증 마감 알림 스케줄링 시작 | 날짜={} 요일={}", today, todayEnum);

        List<Round> targetRounds = roundRepository.findAllByVerificationDay(todayEnum, today);

        log.info("[VerificationDeadlineScheduler] 대상 라운드 {}건", targetRounds.size());

        if (targetRounds.isEmpty()) {
            log.info("[VerificationDeadlineScheduler] 오늘 인증 요일인 라운드 없음");
            return;
        }

        for (Round round : targetRounds) {
            try {
                Challenge challenge = round.getChallenge();

                // verifyStartTime / verifyEndTime(LocalTime) + 오늘 날짜 → LocalDateTime 조합
                LocalDateTime startAt = today.atTime(challenge.getVerifyStartTime());
                LocalDateTime endAt = today.atTime(challenge.getVerifyEndTime());

                eventPublisher.publishEvent(new VerificationDeadlineEvent(
                        round.getId(),
                        challenge.getId(),
                        startAt,
                        endAt
                ));
                log.info("[VerificationDeadlineScheduler] 이벤트 발행 완료 | RoundId={} | {}~{}",
                        round.getId(), startAt, endAt);
            } catch (Exception e) {
                log.error("[VerificationDeadlineScheduler] 이벤트 발행 실패 | RoundId={} | 사유: {}",
                        round.getId(), e.getMessage());
            }
        }
    }
}