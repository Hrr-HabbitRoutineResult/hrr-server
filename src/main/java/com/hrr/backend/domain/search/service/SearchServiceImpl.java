package com.hrr.backend.domain.search.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

	// Redis의 Sorted Set에 사용할 Key
	private static final String POPULAR_SEARCH_KEY = "popular_keywords";

	private final StringRedisTemplate redisTemplate;

	/**
	 * Redis ZINCRBY 명령어 사용: keyword의 count 1만큼 증가
	 */
	@Override
	public void incrementSearchCount(String keyword) {
		ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();

		// count 1만큼 증가. 없으면 최초 생성
		Double score = zSetOperations.incrementScore(POPULAR_SEARCH_KEY, keyword, 1);

		log.info("[SearchServiceImpl] "+keyword+": "+score+"회 검색");
	}

	@Override
	public List<String> getTopNPopularKeywords(int limit) {
		ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();

		// 내림차순(1위부터)으로 조회
		Set<String> topKeywords = zSetOperations.reverseRange(POPULAR_SEARCH_KEY, 0, limit - 1);

		// API 반환을 위해 List로 변환
		return topKeywords != null ? new ArrayList<>(topKeywords) : Collections.emptyList();
	}

}
