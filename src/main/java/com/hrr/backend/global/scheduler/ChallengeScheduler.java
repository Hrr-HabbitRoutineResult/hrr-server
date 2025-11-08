package com.hrr.backend.global.scheduler;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChallengeScheduler {
	private final RedisTemplate<String, String> redisTemplate;
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
}
