package com.hrr.backend.domain.challenge.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.service.ChallengeService;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.SortType;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Challenge", description = "챌린지 관련 API")
@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
public class ChallengeController {

	private final ChallengeService challengeService;

	@GetMapping("")
	@Operation(summary = "챌린지 리스트 조회", description = "챌린지 목록에 필터링을 적용하여 반환합니다.\n"
		+ "e.g. /api/v1/challenges?category=STUDY&isUpcoming=true&sortType=LATEST&day=MONDAY&title=개발&page=1&size=10")
	public ApiResponse<SliceResponseDto<ChallengeResponseDto.InfoDto>> getChallengeList(
		// 필터링 및 정렬
		@RequestParam(name = "category", required = false) Category category,
		@RequestParam(name = "isUpcoming", required = false) Boolean isUpcoming,
		@RequestParam(name = "sortType", defaultValue = "POPULAR", required = false) SortType sortType,
		@RequestParam(name = "day", required = false) List<ChallengeDays> day,
		@RequestParam(name = "title", required = false) String title,

		// 페이징
		@RequestParam(name = "page", defaultValue = "0") int page, // 페이지 번호 (0부터 시작)
		@RequestParam(name = "size", defaultValue = "10") int size  // 페이지 크기
	) {
		// 목록 조회
		SliceResponseDto<ChallengeResponseDto.InfoDto> challenges = challengeService.getChallengeList(
			category,
			isUpcoming,
			sortType,
			day,
			title,
			page,
			size
		);

		return ApiResponse.onSuccess(SuccessCode.OK, challenges);
	}

	@GetMapping("/{challengeId}")
	@Operation(summary = "챌린지 프로필 조회", description = "챌린지 프로필을 조회합니다.")
	public void getChallengeProfile(@PathVariable("challengeId") Long challengeId) {

	}

	@PostMapping("/{challengeId}/click")
	@Operation(summary = "챌린지 클릭 처리", description = "오늘의 인기 챌린지 집계를 위해 챌린지 클릭 시에 카운팅을 진행합니다.")
	public Long clickChallenge(@PathVariable("challengeId") Long challengeId) {

		// 테스트를 위해 임시로 클릭 수 반환

		return challengeService.clickChallenge(challengeId);
	}
}
