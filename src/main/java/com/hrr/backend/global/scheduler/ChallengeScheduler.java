package com.hrr.backend.global.scheduler;

import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeScheduler {

	private final RedisTemplate<String, String> redisTemplate;
	private final ChallengeRepository challengeRepository;

	private static final String TODAY_CHALLENGE_RANKING_KEY = "today:challenge:clicks";

	/**
	 * 매일 자정(00시 00분 00초)에 챌린지별 오늘의 클릭 수 카운트를 초기화
	 */
	@Scheduled(cron = "0 0 0 * * *")
	public void initializeDailyChallengeClicks() {

		// 오늘의 클릭 수를 담고 있는 Sorted Set Key 자체를 삭제
		Boolean deleted = redisTemplate.delete(TODAY_CHALLENGE_RANKING_KEY);
		log.info("[initializeDailyChallengeClicks] 일간 챌린지 클릭 수를 초기화했습니다. redisDeleted={}", deleted);
	}

	/**
	 * 챌린지 상태 변경 (UPCOMING -> ONGOING)
	 * - 기준 시간: 오늘 00:00:00
	 * - startDate가 기준 시간 이전(포함)인 UPCOMING 챌린지를 ONGOING으로 변경
	 */
	@Scheduled(cron = "0 0 0 * * *")
	@Transactional // DB 작업이므로 필수
	public void updateChallengeStatus() {
		// 기준 시간: 오늘 00:00:00
		LocalDateTime referenceTime = LocalDate.now().atStartOfDay();

		// 먼저 어떤 챌린지가 대상인지 ID만 조회
		List<Long> idsToStart = challengeRepository.findIdsToStart(
				ChallengeStatus.UPCOMING,
				referenceTime
		);

		if (idsToStart.isEmpty()) {
			log.info("[updateChallengeStatus] 상태 변경 대상 챌린지가 없습니다. referenceTime={}", referenceTime);
			return;
		}

		// 실제 상태 변경 수행
		int updatedCount = challengeRepository.updateChallengeStatusToOngoing(
				ChallengeStatus.ONGOING,
				ChallengeStatus.UPCOMING,
				referenceTime
		);

		// 변경 로그 (기준 시각 + 변경 건수 + 대상 ID)
		log.info(
			"[updateChallengeStatus] 챌린지 상태 변경을 완료했습니다. referenceTime={}, changedCount={}, challengeIds={}",
				referenceTime,
				updatedCount,
				idsToStart
		);
	}
}
