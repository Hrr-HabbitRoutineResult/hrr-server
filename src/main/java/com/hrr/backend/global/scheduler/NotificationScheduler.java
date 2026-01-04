package com.hrr.backend.global.scheduler;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.notification.event.ChallengeExtensionEvent;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.repository.RoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final RoundRepository roundRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 9 * * *") // 매일 오전 9시
    public void scheduleChallengeExtensionNotifications() {
        LocalDate targetDate = LocalDate.now().plusDays(Challenge.CHALLENGER_DECISION_DAYS);
        List<Round> targetRounds = roundRepository.findAllByEndDate(targetDate);

        for (Round round : targetRounds) {
            eventPublisher.publishEvent(new ChallengeExtensionEvent(round.getId()));
        }
    }
}