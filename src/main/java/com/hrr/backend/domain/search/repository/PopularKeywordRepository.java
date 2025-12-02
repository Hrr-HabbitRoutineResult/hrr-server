package com.hrr.backend.domain.search.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.hrr.backend.domain.search.entity.PopularKeyword;

public interface PopularKeywordRepository extends JpaRepository<PopularKeyword, Long> {

	// total_count 기준으로 상위 N개의 키워드를 조회(N은 service 계층에서 전달)
	@Query("SELECT p.keyword FROM PopularKeyword p ORDER BY p.totalCount DESC"	)
	List<String> findTopKeywordsByCount(Pageable pageable);
}
