package com.hrr.backend.global.scheduler;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.notification.event.ChallengeExtensionResponseEvent;
import com.hrr.backend.domain.notification.event.VerificationDeadlineEvent;
import com.hrr.backend.domain.notification.service.NotificationCommandService;
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

import java.time.Duration;
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
    private final NotificationCommandService notificationCommandService;

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
     * 인증 마감 알림 5분 단위 폴링 스케줄러 (핵심 수정 사항)
     * - 서버 재시작에도 알림이 증발하지 않도록 5분마다 DB 상태를 확인하여 발송
     */
    @Transactional(readOnly = true)
    @Scheduled(fixedRate = 300000) // 5분(300,000ms)마다 실행
    public void checkAndSendVerificationDeadlineNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        ChallengeDays todayEnum = ChallengeDays.from(today.getDayOfWeek());

        // 오늘 인증 요일인 모든 진행 중 라운드 조회
        List<Round> targetRounds = roundRepository.findAllByVerificationDay(todayEnum, today);

        if (targetRounds.isEmpty()) {
            return;
        }

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
                log.error("[VerificationPolling] RoundId: {} 처리 실패: {}", round.getId(), e.getMessage());
            }
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