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

import com.hrr.backend.domain.challenge.converter.ChallengeConverter;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeDayJoinRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.s3.S3UrlUtil;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceImplTest {

	@Mock private ChallengeRepository challengeRepository;
	@Mock private RedisTemplate<String, String> redisTemplate;
	@Mock private ZSetOperations<String, String> zSetOperations;
	@Mock private ChallengeDayJoinRepository challengeDayJoinRepository;
	@Mock private ChallengeConverter challengeConverter;
	@Mock private RoundRepository roundRepository;
	@Mock private RoundRecordRepository roundRecordRepository;
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

	@Test
	@DisplayName("챌린지 라운드 목록 조회 시 로그인 사용자의 라운드별 RoundRecord 존재 여부를 함께 반환한다")
	void getChallengeRounds_ReturnsParticipationStatus() {
		// Arrange
		Long userId = 10L;
		Long challengeId = 1L;
		User user = User.builder().id(userId).build();
		Round round1 = Round.builder().id(101L).roundNumber(1).build();
		Round round2 = Round.builder().id(102L).roundNumber(2).build();
		Challenge challenge = Challenge.builder().id(challengeId).currentRound(round2).build();

		ChallengeResponseDto.RoundDto roundDto1 = ChallengeResponseDto.RoundDto.builder()
				.roundNumber(1)
				.isCurrentRound(false)
				.isParticipated(true)
				.build();
		ChallengeResponseDto.RoundDto roundDto2 = ChallengeResponseDto.RoundDto.builder()
				.roundNumber(2)
				.isCurrentRound(true)
				.isParticipated(false)
				.build();

		given(challengeRepository.findById(challengeId)).willReturn(java.util.Optional.of(challenge));
		given(roundRepository.findAllByChallengeIdOrderByRoundNumberAsc(challengeId)).willReturn(List.of(round1, round2));
		given(roundRecordRepository.findParticipatedRoundNumbers(userId, challengeId))
				.willReturn(List.of(1));
		given(challengeConverter.toRoundDto(round1, false, true)).willReturn(roundDto1);
		given(challengeConverter.toRoundDto(round2, true, false)).willReturn(roundDto2);

		// Act
		List<ChallengeResponseDto.RoundDto> result = challengeService.getChallengeRounds(user, challengeId);

		// Assert
		assertThat(result).containsExactly(roundDto1, roundDto2);
		then(roundRecordRepository).should(times(1))
				.findParticipatedRoundNumbers(userId, challengeId);
	}

	@Test
	@DisplayName("챌린지 라운드 목록 조회 시 챌린지가 없으면 기존 예외를 유지한다")
	void getChallengeRounds_ChallengeNotFound() {
		// Arrange
		Long challengeId = 1L;
		User user = User.builder().id(10L).build();
		given(challengeRepository.findById(challengeId)).willReturn(java.util.Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> challengeService.getChallengeRounds(user, challengeId))
				.isInstanceOf(GlobalException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.CHALLENGE_NOT_FOUND);

		then(roundRepository).shouldHaveNoInteractions();
		then(roundRecordRepository).shouldHaveNoInteractions();
	}
}
