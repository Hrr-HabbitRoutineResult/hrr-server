package com.hrr.backend.global.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.hrr.backend.domain.search.entity.KeywordHourlyLog;
import com.hrr.backend.domain.search.repository.KeywordHourlyLogRepository;
import com.hrr.backend.domain.search.repository.PopularKeywordRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

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
	private final PopularKeywordRepository popularKeywordRepository;

	/**
	 * 매시간 직전 시간대의 검색어 통계를 keyword_hourly_log 테이블에 insert
	 */
	@Transactional
	@Scheduled(cron = "5 0 * * * *")
	public void migrateRedisToLogTable() {
		// 직전 시간(HH-1)의 키를 계산 e.g. 현재 20시면 19시 키 (YYYYMMDD19)를 가져와야 함
		LocalDateTime targetHour = LocalDateTime.now().minusHours(1);
		String targetHourKey = POPULAR_SEARCH_KEY + ":" + targetHour.format(
			java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH")
		);

		try {
			log.info("[SearchScheduler] Redis 마이그레이션 시작. 타켓 시간: {}", targetHourKey);

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
					.count(tuple.getScore() != null ? tuple.getScore().longValue() : 0L)
					.hour(targetHour.withMinute(0).withSecond(0).withNano(0)) // 시각 정보 정리
					.build()
				)
				.filter(log -> log.getKeyword() != null)  // null 키워드 필터링
				.toList();

			// DB에 저장
			keywordHourlyLogRepository.saveAll(logsToSave);
			log.info("[SearchScheduler] {}개 로그 KeywordHourlyLog에 저장 완료.", logsToSave.size());

			// 트랜잭션 커밋 성공 후에만 Redis 키 삭제
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
					Boolean deleted = redisTemplate.delete(targetHourKey);
					log.info("[SearchScheduler] Redis 키 삭제 완료: {} (deleted={})", targetHourKey, deleted);
			}
        });


		} catch (Exception e) {
			log.error("[migrateRedisToLogTable] Redis 마이그레이션 실패. 타켓 시간: {}. 다음에 재시도 필요.", targetHourKey, e);
			// 트랜잭션 롤백으로 DB 저장 취소, Redis 데이터는 보존되어 재시도 가능
			throw new GlobalException(ErrorCode.MIGRATION_REDIS_TO_DB_FAILED, e);
		}

	}

	/**
	 * 매시간 keyword_hourly_log에서 최근 한 달치 검색어 기록을 group by로 집계해서 popular_keyword 테이블로 upsert
	 */
	@Scheduled(cron = "30 0 * * * *")	// 먼저 실행될 migrateRedisToLogTable와의 간격 유지
	@Transactional
	public void aggregateLogToFinalTable() {
		// 현재 시점으로부터 30일 전
		LocalDateTime targetDateTime = LocalDateTime.now().minusDays(30);

		try {
			log.info("[SearchScheduler] 최종 집계 시작. 최근 30일 데이터 기준 시점: {}", targetDateTime);

			// Repository의 UPSERT 쿼리 호출
			// affectedRows = 변경된 레코드의 수
			int affectedRows = popularKeywordRepository.upsertPopularKeywords(targetDateTime);

			log.info("[SearchScheduler] 최종 집계 완료. 총 {}개 키워드 업데이트/생성됨.", affectedRows);
		} catch (Exception e) {
			log.error("[aggregateLogToFinalTable] 최종 집계 실패. 기준 시점: {}.", targetDateTime, e);
			throw new GlobalException(ErrorCode.COUNTING_LOG_TABLE_FAILED, e);
		}

	}

}
