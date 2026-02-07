package com.hrr.backend.domain.challenge.service;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.repository.ChallengeDayJoinRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.global.s3.S3UrlUtil;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceImplTest {

	@Mock private ChallengeRepository challengeRepository;
	@Mock private RedisTemplate<String, String> redisTemplate;
	@Mock private ZSetOperations<String, String> zSetOperations;
	@Mock private ChallengeDayJoinRepository challengeDayJoinRepository;
	@Mock private S3UrlUtil s3UrlUtil;

	@InjectMocks
	private ChallengeServiceImpl challengeService;

	@Test
	@DisplayName("일간 인기 챌린지 조회 시 종료된 챌린지는 제외되어야 한다")
	void getDailyTopChallenges_FilterFinishedChallenges() {
		// Arrange
		int requestedNumber = 5;
		String rankingKey = "today:challenge:clicks";

		given(redisTemplate.opsForZSet()).willReturn(zSetOperations);

		// Redis에 데이터가 2개만 있다고 가정 (size = 2L)
		given(zSetOperations.size(rankingKey)).willReturn(2L);

		Set<ZSetOperations.TypedTuple<String>> mockRedisResult = new LinkedHashSet<>();
		mockRedisResult.add(new DefaultTypedTuple<>("1", 10.0));
		mockRedisResult.add(new DefaultTypedTuple<>("2", 5.0));

		given(zSetOperations.reverseRangeWithScores(eq(rankingKey), anyLong(), anyLong()))
			.willReturn(mockRedisResult);

		// RDB 설정 (1번만 활성 상태)
		ChallengeResponseDto.InfoDto activeChallengeInfo = ChallengeResponseDto.InfoDto.builder()
			.challengeId(1L)
			.title("진행 중인 챌린지")
			.startDate(LocalDateTime.now().plusDays(1))
			.thumbnailUrl("thumb.jpg")
			.build();

		given(challengeRepository.findNotFinishedChallengesByIds(List.of(1L, 2L)))
			.willReturn(List.of(activeChallengeInfo));

		// 나머지 Mock 설정 (S3UrlUtil 등 필드 주입 필요 시 추가)
		given(s3UrlUtil.toFullUrl(any())).willReturn("http://full-url.com");
		given(challengeDayJoinRepository.findByChallengeIdIn(anyList())).willReturn(Collections.emptyList());

		// Act
		List<ChallengeResponseDto.DailyTopDto> result = challengeService.getDailyTopChallenges(requestedNumber);

		// Assert
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getInfo().getChallengeId()).isEqualTo(1L);
		assertThat(result.get(0).getRanking()).isEqualTo(1);
	}
}
