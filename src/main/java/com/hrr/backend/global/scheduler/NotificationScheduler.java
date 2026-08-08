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
            List<Challenge> targetChallenges = challengeRepository
                    .findAllByStartDateGreaterThanEqualAndStartDateLessThan(startInclusive, endExclusive);

            if (targetChallenges.isEmpty()) {
                log.info("[scheduleChallengeStartNotifications] 챌린지 시작 알림 대상이 없습니다. targetDate={}", targetDate);
                return;
            }

            int failCount = 0;
            Exception firstFailure = null;
            for (Challenge challenge : targetChallenges) {
                try {
                    eventPublisher.publishEvent(new ChallengeStartEvent(challenge.getId()));
                } catch (Exception e) {
                    log.warn("[scheduleChallengeStartNotifications] 이벤트 발행에 실패했습니다. challengeId={}",
                            challenge.getId(), e);
                    failCount++;
                    if (firstFailure == null) firstFailure = e;
                }
            }

            if (failCount > 0) {
                log.error("[scheduleChallengeStartNotifications] 알림 이벤트 발행 대상 총 {}건 중 {}건을 실패했습니다.",
                        targetChallenges.size(), failCount, firstFailure);
            }
            log.info("[scheduleChallengeStartNotifications] 챌린지 시작 알림 이벤트 발행을 완료했습니다. targetDate={}, targetCount={}, failedCount={}",
                    targetDate, targetChallenges.size(), failCount);

        } catch (Exception e) {
            log.error("[scheduleChallengeStartNotifications] 챌린지 시작 알림 처리 중 오류가 발생했습니다. targetDate={}",
                    targetDate, e);
        }
    }

    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
    public void scheduleChallengeExtensionNotifications() {
        // 대상 날짜 계산
        LocalDate targetDate = LocalDate.now().plusDays(Challenge.CHALLENGER_DECISION_DAYS);

        try {
            List<Round> targetRounds = roundRepository.findAllByEndDate(targetDate);

            if (targetRounds.isEmpty()) {
                log.info("[scheduleChallengeExtensionNotifications] 챌린지 연장 알림 대상이 없습니다. targetDate={}", targetDate);
                return;
            }

            int failCount = 0;
            Exception firstFailure = null;
            for (Round round : targetRounds) {
                try {
                    eventPublisher.publishEvent(new ChallengeExtensionEvent(round.getId()));
                } catch (Exception e) {
                    // 개별 라운드 처리 중 예외 발생 시 경고 로깅
                    log.warn("[scheduleChallengeExtensionNotifications] 이벤트 발행에 실패했습니다. roundId={}",
                            round.getId(), e);
                    failCount++;
                    if (firstFailure == null) firstFailure = e;
                }
            }

            if (failCount > 0) {
                log.error("[scheduleChallengeExtensionNotifications] 알림 이벤트 발행 대상 총 {}건 중 {}건을 실패했습니다.",
                        targetRounds.size(), failCount, firstFailure);
            }
            log.info("[scheduleChallengeExtensionNotifications] 챌린지 연장 알림 이벤트 발행을 완료했습니다. targetDate={}, targetCount={}, failedCount={}",
                    targetDate, targetRounds.size(), failCount);

        } catch (Exception e) {
            // 전체 프로세스 중 예외 발생 시 에러 로깅
            log.error("[scheduleChallengeExtensionNotifications] 챌린지 연장 알림 처리 중 오류가 발생했습니다. targetDate={}",
                    targetDate, e);
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
        Exception firstFailure = null;
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
                log.warn("[checkAndSendVerificationDeadlineNotifications] 인증 마감 알림 처리에 실패했습니다. roundId={}",
                        round.getId(), e);
                failCount++;
                if (firstFailure == null) firstFailure = e;
            }
        }

        if (failCount > 0) {
            log.error("[checkAndSendVerificationDeadlineNotifications] 알림 처리 대상 총 {}건 중 {}건을 실패했습니다.",
                    targetRounds.size(), failCount, firstFailure);
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
