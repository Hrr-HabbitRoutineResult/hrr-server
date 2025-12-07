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
		Boolean isDeleted = redisTemplate.delete(TODAY_CHALLENGE_RANKING_KEY);

		if (isDeleted) {
			log.info("00시 초기화 완료. 클릭수 Sorted Set 삭제됨.");
		}
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

		// 1) 먼저 어떤 챌린지가 대상인지 ID만 조회
		List<Long> idsToStart = challengeRepository.findIdsToStart(
				ChallengeStatus.UPCOMING,
				referenceTime
		);

		if (idsToStart.isEmpty()) {
			log.info("[Scheduler] 챌린지 상태 변경 대상 없음. 기준 시각: {}", referenceTime);
			return;
		}

		// 2) 실제 상태 변경 수행
		int updatedCount = challengeRepository.updateChallengeStatusToOngoing(
				ChallengeStatus.ONGOING,
				ChallengeStatus.UPCOMING,
				referenceTime
		);

		// 3) 변경 로그 (기준 시각 + 변경 건수 + 대상 ID)
		log.info(
				"[Scheduler] 챌린지 상태 변경 완료. 기준 시각: {}, 변경 건수: {}, 대상 ID: {}",
				referenceTime,
				updatedCount,
				idsToStart
		);
	}
}
