package com.hrr.backend.domain.challenge.service;


import java.util.List;

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
	Integer getChallengeProfile(Long challengeId);

}
