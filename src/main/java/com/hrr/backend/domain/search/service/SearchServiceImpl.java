package com.hrr.backend.domain.search.service;

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
	public void incrementSearchCount(String keyword) {
		ZSetOperations<String, String> zSetOperations = redisTemplate.opsForZSet();

		// count 1만큼 증가. 없으면 최초 생성
		Double score = zSetOperations.incrementScore(POPULAR_SEARCH_KEY, keyword, 1);

		log.info("[SearchServiceImpl] "+keyword+": "+score+"회 검색");
	}

}
