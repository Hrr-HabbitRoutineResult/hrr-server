package com.hrr.backend.domain.challenge.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.SortType;
import com.hrr.backend.global.response.SliceResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

	private final ChallengeRepository challengeRepository;

	private static final int UPCOMING_DAYS_CRITERIA = 5;	// '곧 시작' 챌린지 판단 기준 일자

	@Override
	public SliceResponseDto<ChallengeResponseDto.InfoDto> getChallengeList(
		Category category,
		Boolean isUpcoming,
		SortType sortType,
		List<ChallengeDays> days,
		String title,
		int page,
		int size
	) {
		// isUpcoming에 따라 '곧 시작' 유효날짜 범위 계산
		LocalDateTime upcomingStartDate = null;
		LocalDateTime upcomingEndDate = null;

		if (isUpcoming != null && isUpcoming) {
			LocalDate today = LocalDate.now();

			// 시작일
			upcomingStartDate = today.atStartOfDay();

			// 종료일 ; 오늘 + 5일
			upcomingEndDate = today.plusDays(UPCOMING_DAYS_CRITERIA)
				.atTime(23, 59, 59, 999_999_999);
		}

		Pageable pageable = PageRequest.of(page, size);

		// Repository 호출
		Slice<ChallengeResponseDto.InfoDto> tempDtoSlice = challengeRepository.findChallengesWithFilters(
			category,
			upcomingStartDate,
			upcomingEndDate,
			sortType,
			days,
			title,
			pageable
		);

		// Repository에서 조회해 온 dto 기반으로 결과 dto 필드 일부 채우기
		Slice<ChallengeResponseDto.InfoDto> finalDtoSlice = tempDtoSlice.map(tempDto -> {

			LocalDate challengeStartDate = tempDto.getStartDate().toLocalDate();
			LocalDate today = LocalDate.now();

			long dDay = ChronoUnit.DAYS.between(today, challengeStartDate);
			boolean isUpcomingResult = (dDay >= 0) && (dDay <= UPCOMING_DAYS_CRITERIA);

			// dto 나머지 필드 채우기(isUpcoming, dDayUntilStart)
			tempDto.setIsUpcoming(isUpcomingResult);
			tempDto.setDDayUntilStart((int)Math.max(0, dDay));	// 시작 전일 경우에만 남은 날짜를 set, else 0

			return tempDto;
		});

		return new SliceResponseDto<>(finalDtoSlice);
	}
}
