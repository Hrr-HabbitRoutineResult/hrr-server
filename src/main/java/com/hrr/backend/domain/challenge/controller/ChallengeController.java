package com.hrr.backend.domain.challenge.controller;

import java.util.List;

import com.hrr.backend.domain.challenge.dto.ChallengeRequestDto;
import com.hrr.backend.global.config.CustomUserDetails;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.service.ChallengeService;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.SortType;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "Challenge", description = "챌린지 관련 API")
@RestController
@RequestMapping("/api/v1/challenges")
@RequiredArgsConstructor
@Validated
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
		@Min(1)
		@RequestParam(name = "page", defaultValue = "1") int page, // 페이지 번호 (1-based)
		@RequestParam(name = "size", defaultValue = "10") int size  // 페이지 크기
	) {
		// 목록 조회
		SliceResponseDto<ChallengeResponseDto.InfoDto> challenges = challengeService.getChallengeList(
			category,
			isUpcoming,
			sortType,
			day,
			title,
			page-1,
			size
		);

		return ApiResponse.onSuccess(SuccessCode.OK, challenges);
	}

	@GetMapping("/{challengeId}/info")
	@Operation(summary = "챌린지 상세 상단 정보 조회", description = "챌린지 기본 정보, 방장, 내 참여 상태, 버튼 상태 등을 조회합니다.")
	public ApiResponse<ChallengeResponseDto.HeaderInfoDto> getChallengeHeaderInfo(
			@PathVariable("challengeId") Long challengeId,
			@Parameter(hidden = true)
			@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ChallengeResponseDto.HeaderInfoDto response = challengeService.getChallengeHeaderInfo(
				challengeId,
				userDetails.getUser()
		);
		return ApiResponse.onSuccess(SuccessCode.OK, response);
	}

	@GetMapping("/{challengeId}/profile")
	@Operation(summary = "챌린지 프로필 조회 (참여 전/후 UI 통합)",
			description = "로그인한 유저의 참여 상태에 따라 인증 현황을 포함하거나 제외하여 반환합니다.")
	public ApiResponse<ChallengeResponseDto.ChallengeProfileDto> getChallengeProfile(
			@PathVariable("challengeId") Long challengeId,
			@Parameter(hidden = true)
			@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ChallengeResponseDto.ChallengeProfileDto response = challengeService.getChallengeProfile(
				userDetails.getUser(),
				challengeId
		);
		return ApiResponse.onSuccess(SuccessCode.OK, response);
	}

	@PostMapping("/{challengeId}/click")
	@Operation(summary = "챌린지 클릭 처리", description = "오늘의 인기 챌린지 집계를 위해 챌린지 클릭 시에 카운팅을 진행합니다.")
	public ApiResponse<Long> clickChallenge(@PathVariable("challengeId") Long challengeId) {

		return ApiResponse.onSuccess(SuccessCode.OK, challengeService.clickChallenge(challengeId));
	}

	@GetMapping("/daily-top")
	@Operation(summary = "오늘의 인기 챌린지 조회", description = "오늘의 클릭 수를 기준으로 상위 n개 챌린지를 조회합니다.")
	public ApiResponse<List<ChallengeResponseDto.DailyTopDto>> getDailyTopChallenges(
		@RequestParam("number")
		@Min(value = 1, message = "조회 개수는 1 이상이어야 합니다")
		@Max(value = 50, message = "성능상 조회 개수는 50 이하로 제한하고 있습니다.") int number) {

		return ApiResponse.onSuccess(SuccessCode.OK, challengeService.getDailyTopChallenges(number));
	}

	@Operation(summary = "챌린지 생성", description = "챌린지를 생성합니다. 챌린지를 생성하는 동시에 방장으로 참여하게 됩니다.")
	@PostMapping("")
	public ApiResponse<ChallengeResponseDto.CreateChallengeDto> createChallenge(
			@Parameter(hidden = true)
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody ChallengeRequestDto.CreateChallengeDto request
	) {
		return ApiResponse.onSuccess(
				SuccessCode.OK,
				challengeService.createChallenge(userDetails.getUser(), request)
		);
	}

	@Operation(
			summary = "챌린지 참가",
			description = "사용자가 특정 챌린지에 참가합니다. 비공개 챌린지인 경우 비밀번호 검증이 수행됩니다."
	)
	@PostMapping("/{challengeId}/join")
	public ApiResponse<ChallengeResponseDto.JoinChallengeDto> joinChallenge(
			@Parameter(hidden = true)
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("challengeId") Long challengeId,
			@Valid @RequestBody ChallengeRequestDto.JoinChallengeDto request
	) {
		return ApiResponse.onSuccess(
				SuccessCode.OK,
				challengeService.joinChallenge(userDetails.getUser(), challengeId, request)
		);
	}

	@PostMapping("/{challengeId}/wait")
	@Operation(summary = "챌린지 공석 알림 신청", description = "참여 인원이 마감된 챌린지에 공석이 생길 시 알림을 받기 위해 대기 신청을 합니다.")
	public ApiResponse<Void> registerChallengeWait(
			@PathVariable("challengeId") Long challengeId,
			@Parameter(hidden = true)
			@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		challengeService.registerChallengeWait(userDetails.getUser(), challengeId);
		return ApiResponse.onSuccess(SuccessCode.CHALLENGE_WAIT_REGISTER_OK, null);
	}

	@DeleteMapping("/{challengeId}/wait")
	@Operation(summary = "챌린지 공석 알림 취소", description = "신청했던 챌린지 오픈 알림을 취소합니다.")
	public ApiResponse<Void> cancelChallengeWait(
			@PathVariable("challengeId") Long challengeId,
			@Parameter(hidden = true)
			@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		challengeService.cancelChallengeWait(userDetails.getUser(), challengeId);
		return ApiResponse.onSuccess(SuccessCode.CHALLENGE_WAIT_CANCEL_OK, null);
	}

	@PostMapping("/{challengeId}/likes")
	@Operation(summary = "챌린지 좋아요 등록", description = "챌린지에 좋아요를 표시하고, 갱신된 좋아요 수와 상태를 반환합니다.")
	public ApiResponse<ChallengeResponseDto.ChallengeLikeDto> likeChallenge(
			@Parameter(hidden = true)
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("challengeId") Long challengeId
	) {
		ChallengeResponseDto.ChallengeLikeDto response = challengeService.likeChallenge(userDetails.getUser(), challengeId);
		return ApiResponse.onSuccess(SuccessCode.OK, response);
	}

	@DeleteMapping("/{challengeId}/likes")
	@Operation(summary = "챌린지 좋아요 취소", description = "챌린지 좋아요를 취소하고, 갱신된 좋아요 수와 상태를 반환합니다.")
	public ApiResponse<ChallengeResponseDto.ChallengeLikeDto> unlikeChallenge(
			@Parameter(hidden = true)
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable("challengeId") Long challengeId
	) {
		ChallengeResponseDto.ChallengeLikeDto response = challengeService.unlikeChallenge(userDetails.getUser(), challengeId);
		return ApiResponse.onSuccess(SuccessCode.OK, response);
	}

	@GetMapping("/{challengeId}/rounds")
	@Operation(summary = "챌린지 라운드 목록 조회", description = "챌린지의 전체 라운드 목록(1R, 2R...)을 회차순으로 반환합니다. 현재 진행 중인 라운드는 isCurrentRound=true로 표시됩니다.")
	public ApiResponse<List<ChallengeResponseDto.RoundDto>> getChallengeRounds(
			@PathVariable("challengeId") Long challengeId
	) {
		List<ChallengeResponseDto.RoundDto> response = challengeService.getChallengeRounds(challengeId);

		return ApiResponse.onSuccess(SuccessCode.OK, response);
	}
}
