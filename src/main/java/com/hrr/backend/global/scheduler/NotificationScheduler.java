package com.hrr.backend.global.scheduler;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeStartEvent;
import com.hrr.backend.domain.notification.event.VerificationDeadlineEvent;
import com.hrr.backend.domain.notification.service.NotificationCommandService;
import com.hrr.backend.domain.round.entity.Round;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final ChallengeRepository challengeRepository;
    private final RoundRepository roundRepository;
    private final RoundRecordRepository roundRecordRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationCommandService notificationCommandService;

    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
    public void scheduleChallengeStartNotifications() {
        LocalDate targetDate = LocalDate.now().plusDays(1);
        LocalDateTime startInclusive = targetDate.atStartOfDay();
        LocalDateTime endExclusive = targetDate.plusDays(1).atStartOfDay();

        try {
            log.info("[ChallengeStartScheduler] 챌린지 시작 하루 전 알림 스케줄러 시작 (대상 날짜: {})", targetDate);

            List<Challenge> targetChallenges = challengeRepository
                    .findAllByStartDateGreaterThanEqualAndStartDateLessThan(startInclusive, endExclusive);

            log.info("[ChallengeStartScheduler] 대상 날짜({})에 시작하는 챌린지 총 {}건 발견", targetDate, targetChallenges.size());

            if (targetChallenges.isEmpty()) {
                log.info("[ChallengeStartScheduler] 알림을 보낼 대상 챌린지가 없습니다.");
                return;
            }

            int failCount = 0;
            for (Challenge challenge : targetChallenges) {
                try {
                    eventPublisher.publishEvent(new ChallengeStartEvent(challenge.getId()));
                    log.info("[ChallengeStartScheduler] 이벤트 발행 완료 - ChallengeId: {}", challenge.getId());
                } catch (Exception e) {
                    log.warn("[ChallengeStartScheduler] 이벤트 발행 실패 - ChallengeId: {}, 사유: {}",
                            challenge.getId(), e.getMessage());
                    failCount++;
                }
            }

            if (failCount > 0) {
                log.error("[ChallengeStartScheduler] 이벤트 발행 총 {}건 중 {}건 실패", targetChallenges.size(), failCount);
            }

            log.info("[ChallengeStartScheduler] 스케줄러 작업 정상 종료");

        } catch (Exception e) {
            log.error("[ChallengeStartScheduler] 스케줄러 실행 중 심각한 오류 발생 (대상 날짜: {}), 에러: ", targetDate, e);
        }
    }

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

            int failCount = 0;
            for (Round round : targetRounds) {
                try {
                    // 각 라운드별 이벤트 발행 시도 로깅
                    eventPublisher.publishEvent(new ChallengeExtensionEvent(round.getId()));
                    log.info("[ChallengeExtensionScheduler] 이벤트 발행 완료 - RoundId: {}", round.getId());
                } catch (Exception e) {
                    // 개별 라운드 처리 중 예외 발생 시 경고 로깅
                    log.warn("[ChallengeExtensionScheduler] 이벤트 발행 실패 - RoundId: {}, 사유: {}", round.getId(), e.getMessage());
                    failCount++;
                }
            }

            if (failCount > 0) {
                log.error("[ChallengeExtensionScheduler] 이벤트 발행 총 {}건 중 {}건 실패", targetRounds.size(), failCount);
            }

            log.info("[ChallengeExtensionScheduler] 스케줄러 작업 정상 종료");

        } catch (Exception e) {
            // 전체 프로세스 중 예외 발생 시 에러 로깅
            log.error("[ChallengeExtensionScheduler] 스케줄러 실행 중 심각한 오류 발생 (대상 날짜: {}), 에러: ", targetDate, e);
        }
    }

    /**
     * 인증 마감 알림 5분 단위 폴링 스케줄러 (핵심 수정 사항)
     * - 서버 재시작에도 알림이 증발하지 않도록 5분마다 DB 상태를 확인하여 발송
     */
    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0/5 * * * *")
    public void checkAndSendVerificationDeadlineNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        ChallengeDays todayEnum = ChallengeDays.from(today.getDayOfWeek());

        // 오늘 인증 요일인 모든 진행 중 라운드 조회
        List<Round> targetRounds = roundRepository.findAllByVerificationDay(todayEnum, today);

        if (targetRounds.isEmpty()) {
            return;
        }

        int failCount = 0;
        for (Round round : targetRounds) {
            try {
                Challenge challenge = round.getChallenge();
                LocalDateTime startAt = today.atTime(challenge.getVerifyStartTime());
                LocalDateTime endAt = today.atTime(challenge.getVerifyEndTime());
                Duration window = Duration.between(startAt, endAt);

                LocalDateTime deadline3h = endAt.minusHours(3);
                LocalDateTime deadline1h = endAt.minusHours(1);

                // 알림 윈도우 크기에 따른 발송 로직 분기
                if (window.toHours() >= 3) {
                    // [3시간 전 알림] 마감 3시간 전 ~ 1시간 전 사이
                    if (now.isAfter(deadline3h) && now.isBefore(deadline1h)) {
                        sendNotification(round, challenge, NotificationTypeName.VERIFICATION_DEADLINE_3H, startAt, endAt, today);
                    }
                    // [1시간 전 알림] 마감 1시간 전 ~ 마감 시각 사이
                    if (now.isAfter(deadline1h) && now.isBefore(endAt)) {
                        sendNotification(round, challenge, NotificationTypeName.VERIFICATION_DEADLINE_1H, startAt, endAt, today);
                    }
                } else if (window.toHours() >= 1) {
                    // [1시간 전 알림] 1시간 전 ~ 마감 시각 사이
                    if (now.isAfter(deadline1h) && now.isBefore(endAt)) {
                        sendNotification(round, challenge, NotificationTypeName.VERIFICATION_DEADLINE_1H, startAt, endAt, today);
                    }
                } else {
                    // [마감 임박 알림] 윈도우가 1시간 미만인 경우
                    if (now.isAfter(startAt) && now.isBefore(endAt)) {
                        sendNotification(round, challenge, NotificationTypeName.VERIFICATION_DEADLINE_NOW, startAt, endAt, today);
                    }
                }
            } catch (Exception e) {
                log.warn("[VerificationPolling] RoundId: {} 처리 실패: {}", round.getId(), e.getMessage());
                failCount++;
            }
        }

        if (failCount > 0) {
            log.error("[VerificationPolling] 처리 총 {}건 중 {}건 실패", targetRounds.size(), failCount);
        }
    }

    /**
     * 알림 명령을 직접 전달하는 헬퍼 메서드
     */
    private void sendNotification(Round round, Challenge challenge,
                                  NotificationTypeName typeName,
                                  LocalDateTime startAt, LocalDateTime endAt,
                                  LocalDate targetDate) {
        VerificationDeadlineEvent event = new VerificationDeadlineEvent(
                round.getId(), challenge.getId(), startAt, endAt
        );
        // 리스너를 거치지 않고 즉시 서비스 호출
        notificationCommandService.sendDeadlineNotification(event, typeName, targetDate);
    }
}
