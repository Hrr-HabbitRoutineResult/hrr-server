package com.hrr.backend.domain.challenge.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.challenge.repository.ChallengeDayJoinRepository;
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
	private final ChallengeDayJoinRepository challengeDayJoinRepository;

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

		// Repository 호출 - 필터 적용된 챌린지들 반환(요일, 곧 시작 여부, 시작까지 남은 일자는 제외)
		Slice<ChallengeResponseDto.InfoDto> tempDtoSlice = challengeRepository.findChallengesWithFilters(
			category,
			upcomingStartDate,
			upcomingEndDate,
			sortType,
			days,
			title,
			pageable
		);

		// ---응답 dto에 추가할 요일 정보 조회---
		// 모든 challengeId 추출
		List<Long> challengeIds = tempDtoSlice.getContent().stream()
			.map(ChallengeResponseDto.InfoDto::getChallengeId)
			.toList();

		// 해당 challenge들을 가진 ChallengeDayJoin 엔티티 조회
		List<ChallengeDayJoin> allDays = challengeDayJoinRepository.findByChallengeIdIn(challengeIds);

		// 챌린지 아이디-요일 리스트 매핑
		Map<Long, List<ChallengeDays>> daysMap = allDays.stream()
			.collect(Collectors.groupingBy(
				dayJoin -> dayJoin.getChallenge().getId(), // key: 챌린지 아이디
				Collectors.mapping(ChallengeDayJoin::getDayOfWeek, Collectors.toList()) // value: 요일 리스트
			));


		// Repository에서 조회해 온 dto 기반으로 결과 dto 필드 일부 채우기
		Slice<ChallengeResponseDto.InfoDto> finalDtoSlice = tempDtoSlice.map(tempDto -> {

			LocalDate challengeStartDate = tempDto.getStartDate().toLocalDate();
			LocalDate today = LocalDate.now();

			long dDay = ChronoUnit.DAYS.between(today, challengeStartDate);
			boolean isUpcomingResult = (dDay >= 0) && (dDay <= UPCOMING_DAYS_CRITERIA);

			// dto 나머지 필드 채우기(isUpcoming, dDayUntilStart, dayOfWeek)
			tempDto.setIsUpcoming(isUpcomingResult);
			tempDto.setDDayUntilStart((int)Math.max(0, dDay));	// 시작 전일 경우에만 남은 날짜를 set, else 0
			tempDto.setDaysOfWeek(daysMap.getOrDefault(tempDto.getChallengeId(), List.of()));

			return tempDto;
		});

		return new SliceResponseDto<>(finalDtoSlice);
	}
}
