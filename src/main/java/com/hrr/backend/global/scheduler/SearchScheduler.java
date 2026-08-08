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
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.hrr.backend.domain.search.entity.KeywordHourlyLog;
import com.hrr.backend.domain.search.repository.KeywordHourlyLogRepository;
import com.hrr.backend.domain.search.repository.PopularKeywordRepository;
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
		log.info("[migrateRedisToLogTable] Redis 검색어 로그의 DB 마이그레이션을 시작합니다. targetHour={}", targetHourKey);

		try {
			// Redis에서 해당 ZSET의 모든 keyword, count 조회
			ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();
			Set<ZSetOperations.TypedTuple<String>> entries = zSetOps.reverseRangeWithScores(targetHourKey, 0, -1);

			if (entries == null || entries.isEmpty()) {
				log.info("[migrateRedisToLogTable] 마이그레이션 대상 Redis 검색어 로그가 없습니다. targetHour={}", targetHourKey);
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
			// 트랜잭션 커밋 성공 후에만 Redis 키 삭제
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
					try {
						Boolean deleted = redisTemplate.delete(targetHourKey);
						log.info("[migrateRedisToLogTable] Redis 검색어 로그의 DB 저장을 완료했습니다. logCount={}, redisDeleted={}",
							logsToSave.size(), deleted);
					} catch (Exception e) {
						// DB 커밋은 이미 끝났으므로 재던지지 않고, 남은 Redis 키의 수동 정리가 필요함을 한 번 알린다.
						log.error("[migrateRedisToLogTable] DB commit 후 Redis key 삭제에 실패했습니다. key={}",
							targetHourKey, e);
					}
			}
        });


		} catch (Exception e) {
			log.error("[migrateRedisToLogTable] Redis 데이터를 DB로 마이그레이션하는 중 오류가 발생했습니다. targetHour={}",
				targetHourKey, e);
			// 예외를 다시 던지면 Spring Scheduler도 같은 실패를 ERROR로 기록해 Discord 알림이 중복된다.
			// 현재 트랜잭션만 rollback-only로 표시해 DB 저장을 취소하고 Redis 데이터는 다음 재시도를 위해 보존한다.
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
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
		log.info("[aggregateLogToFinalTable] 검색어 최종 집계를 시작합니다. targetDateTime={}", targetDateTime);

		try {
			// Repository의 UPSERT 쿼리 호출
			// affectedRows = 변경된 레코드의 수
			int affectedRows = popularKeywordRepository.upsertPopularKeywords(targetDateTime);

			log.info("[aggregateLogToFinalTable] 검색어 최종 집계를 완료했습니다. affectedCount={}", affectedRows);
		} catch (Exception e) {
			log.error("[aggregateLogToFinalTable] 검색어 최종 집계에 실패했습니다. targetDateTime={}",
				targetDateTime, e);
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
		}

	}

}
