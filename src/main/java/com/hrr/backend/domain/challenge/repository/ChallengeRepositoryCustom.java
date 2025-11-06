package com.hrr.backend.domain.challenge.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.SortType;

public interface ChallengeRepositoryCustom {

	// 필터링을 적용하여 챌린지 리스트 조회
	Slice<ChallengeResponseDto.InfoDto> findChallengesWithFilters(
		Category category,
		LocalDateTime upcomingStartDate,
		LocalDateTime upcomingEndDate,
		SortType sortType,
		List<ChallengeDays> days,
		String title,
		Pageable pageable
	);
}
