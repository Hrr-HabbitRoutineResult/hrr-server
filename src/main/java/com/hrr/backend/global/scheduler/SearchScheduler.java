package com.hrr.backend.global.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrr.backend.domain.search.entity.KeywordHourlyLog;
import com.hrr.backend.domain.search.repository.KeywordHourlyLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchScheduler {

	// Redis의 Sorted Set에 사용할 Key
	private static final String POPULAR_SEARCH_KEY = "popular_keywords";
	private final StringRedisTemplate redisTemplate;

	private final KeywordHourlyLogRepository keywordHourlyLogRepository;

	/**
	 * 매시간 직전 시간대의 검색어 통계를 keyword_hourly_log 테이블에 insert
	 */
	@Scheduled(cron = "0 2 * * * *")
	public void migrateRedisToLogTable() {
		// 직전 시간(HH-1)의 키를 계산 e.g. 현재 20시면 19시 키 (YYYYMMDD19)를 가져와야 함
		LocalDateTime targetHour = LocalDateTime.now().minusHours(1);
		String targetHourKey = POPULAR_SEARCH_KEY + ":" + targetHour.format(
			java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH")
		);

		log.info("[SearchScheduler] Redis 마이그레이션 시작. 타켓: {}", targetHourKey);

		// Redis에서 해당 ZSET의 모든 keyword, count 조회
		ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
		Set<ZSetOperations.TypedTuple<String>> entries = zSetOps.reverseRangeWithScores(targetHourKey, 0, -1);

		if (entries == null || entries.isEmpty()) {
			log.warn("[SearchScheduler] 타켓에서 조회된 데이터 없음: {}", targetHourKey);
			return;
		}

		// Entity로 변환 및 DB Insert 준비
		List<KeywordHourlyLog> logsToSave = entries.stream()
			.map(tuple -> KeywordHourlyLog.builder()
				.keyword(tuple.getValue())
				.count(Objects.requireNonNull(tuple.getScore()).longValue())
				.hour(targetHour.withMinute(0).withSecond(0).withNano(0)) // 시각 정보 정리
				.build()
			)
			.toList();

		// DB에 저장
		keywordHourlyLogRepository.saveAll(logsToSave);
		log.info("[SearchScheduler] {}개 로그 KeywordHourlyLog에 저장 완료.", logsToSave.size());

		// 처리 완료된 Redis 키 삭제
		redisTemplate.delete(targetHourKey);
		log.info("[SearchScheduler] Redis 키 삭제 완료: {}", targetHourKey);
	}

}
