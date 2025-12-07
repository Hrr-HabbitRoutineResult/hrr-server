package com.hrr.backend.domain.search.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrr.backend.domain.search.entity.PopularKeyword;

public interface PopularKeywordRepository extends JpaRepository<PopularKeyword, Long> {

	// total_count 기준으로 상위 N개의 키워드를 조회(N은 service 계층에서 전달)
	@Query("SELECT p.keyword FROM PopularKeyword p ORDER BY p.totalCount DESC"	)
	List<String> findTopKeywordsByCount(Pageable pageable);

	/**
	 * 로그 테이블에서 최근 30일 데이터를 집계하여 PopularKeyword 테이블에 UPSERT 수행
	 * MySQL의 INSERT ... ON DUPLICATE KEY UPDATE 구문을 사용합니다.
	 * @param targetDateTime 현재 시간으로부터 30일 전 시점 (필터링 기준)
	 */
	@Modifying
	@Query(value = """
        INSERT INTO popular_keyword (keyword, total_count)
        SELECT 
            log.keyword, 
            SUM(log.count) as new_total_count 
        FROM keyword_hourly_log log
        WHERE log.hour >= :targetDateTime  -- 최근 30일 필터링
        GROUP BY log.keyword
        ON DUPLICATE KEY UPDATE 
            total_count = VALUES(total_count) -- 기존 레코드의 total_count를 새로운 합계로 덮어씀
    """, nativeQuery = true)
	int upsertPopularKeywords(@Param("targetDateTime") LocalDateTime targetDateTime);
}
