package com.hrr.backend.domain.challenge.service;


import java.util.List;

import com.hrr.backend.domain.challenge.dto.ChallengeRequestDto;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.SortType;
import com.hrr.backend.global.response.SliceResponseDto;

public interface ChallengeService {

	// 챌린지 리스트 조회
	SliceResponseDto<ChallengeResponseDto.InfoDto> getChallengeList(
		Category category,
		Boolean isUpcoming,
		SortType sortType,
		List<ChallengeDays> days,
		String title,
		int page,
		int size
	);

	// 챌린지 프로필 조회
	//Integer getChallengeProfile(Long challengeId);

	// 챌린지 클릭 처리
	Long clickChallenge(Long challengeId);

	/**
 * Retrieves today's most popular challenges limited to the specified count.
 *
 * @param number the maximum number of top challenges to return
 * @return a list of ChallengeResponseDto.DailyTopDto representing today's top challenges, up to {@code number} items
 */
	List<ChallengeResponseDto.DailyTopDto> getDailyTopChallenges(int number);

	/**
	 * Creates a new challenge for the specified user.
	 *
	 * @param userId     the ID of the user who creates the challenge
	 * @param requestDto the details of the challenge to create
	 * @return           the created challenge details
	 */
	ChallengeResponseDto.CreateChallengeDto createChallenge(
			Long userId,
			ChallengeRequestDto.CreateChallengeDto requestDto
	);

	/**
	 * Processes a user's request to join a challenge.
	 *
	 * @param userId      the ID of the user who is joining the challenge
	 * @param challengeId the ID of the challenge to join
	 * @param requestDto  details required to join the challenge
	 * @return            a JoinChallengeDto containing the resulting membership details
	 */
	ChallengeResponseDto.JoinChallengeDto joinChallenge(
			Long userId,
			Long challengeId,
			ChallengeRequestDto.JoinChallengeDto requestDto
	);

}