package com.hrr.backend.domain.search.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.hrr.backend.domain.search.repository.PopularKeywordRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

	// Redis의 Sorted Set에 사용할 Key
	private static final String POPULAR_SEARCH_KEY = "popular_keywords";

	private final StringRedisTemplate redisTemplate;

	private final PopularKeywordRepository popularKeywordRepository;

	/**
	 * Redis ZINCRBY 명령어 사용: keyword의 count 1만큼 증가
	 */
	@Override
	public void incrementSearchCount(String keyword) {
		ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();

		// 시간대를 key로 지정
		String hourKey = POPULAR_SEARCH_KEY+":"+java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHH"));

		// count 1만큼 증가. 없으면 최초 생성
		Double score = zSetOperations.incrementScore(hourKey, keyword, 1);

		log.info("[SearchServiceImpl, 인기 검색어] "+keyword+": "+score+"회 검색");
	}

	@Override
	public List<String> getTopNPopularKeywords(int limit) {
		Pageable pageable = PageRequest.of(0, limit);

		return popularKeywordRepository.findTopKeywordsByCount(pageable);
	}

}
