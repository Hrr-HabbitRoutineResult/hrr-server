package com.hrr.backend.domain.follow.controller;

import com.hrr.backend.domain.follow.dto.FollowListResponseDto;
import com.hrr.backend.domain.follow.dto.FollowResponseDto;
import com.hrr.backend.domain.follow.service.FollowListService;
import com.hrr.backend.domain.follow.service.FollowService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/follow")
@Tag(name = "Follow", description = "팔로우 관련 API")
public class FollowController {

	private final FollowService followService;
	private final FollowListService followListService;

	// ===== 팔로우/언팔로우 =====

	@Operation(summary = "사용자 팔로우", description = "특정 사용자를 팔로우합니다.")
	@PostMapping("/{followedUserId}")
	public ApiResponse<FollowResponseDto> followUser(
		@Parameter(description = "팔로우할 사용자 ID", required = true)
		@PathVariable Long followedUserId,
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {
		Long currentUserId = customUserDetails.getUser().getId();
		log.info("팔로우 요청 - currentUserId: {}, followedUserId: {}", currentUserId, followedUserId);

		FollowResponseDto response = followService.followUser(currentUserId, followedUserId);
		return ApiResponse.onSuccess(SuccessCode.OK, response);
	}

	@Operation(summary = "사용자 팔로우 취소", description = "특정 사용자 팔로우를 취소합니다.")
	@DeleteMapping("/{unfollowedUserId}")
	public ApiResponse<FollowResponseDto> unfollowUser(
		@Parameter(description = "팔로우 취소할 사용자 ID", required = true)
		@PathVariable Long unfollowedUserId,
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {
		Long currentUserId = customUserDetails.getUser().getId();
		log.info("팔로우 취소 요청 - currentUserId: {}, unfollowedUserId: {}", currentUserId, unfollowedUserId);

		FollowResponseDto response = followService.unfollowUser(currentUserId, unfollowedUserId);
		return ApiResponse.onSuccess(SuccessCode.OK, response);
	}

	// ===== 내 팔로우 정보 조회 =====

	@Operation(summary = "내 팔로잉 목록 조회", description = "현재 로그인한 사용자의 팔로잉 목록을 조회합니다.")
	@GetMapping("/me/followings")
	public ApiResponse<SliceResponseDto<FollowListResponseDto>> getMyFollowings(
		@Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
		@RequestParam(defaultValue = "0") int page,
		@Parameter(description = "페이지 크기", example = "20")
		@RequestParam(defaultValue = "20") int size,
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {
		Long currentUserId = customUserDetails.getUser().getId();
		log.info("내 팔로잉 목록 조회 요청 - currentUserId: {}, page: {}, size: {}", currentUserId, page, size);

		Pageable pageable = PageRequest.of(page, size);
		SliceResponseDto<FollowListResponseDto> followings = followListService.getFollowings(currentUserId, currentUserId, pageable);
		return ApiResponse.onSuccess(SuccessCode.OK, followings);
	}

	@Operation(summary = "내 팔로워 목록 조회", description = "현재 로그인한 사용자의 팔로워 목록을 조회합니다.")
	@GetMapping("/me/followers")
	public ApiResponse<SliceResponseDto<FollowListResponseDto>> getMyFollowers(
		@Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
		@RequestParam(defaultValue = "0") int page,
		@Parameter(description = "페이지 크기", example = "20")
		@RequestParam(defaultValue = "20") int size,
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {
		Long currentUserId = customUserDetails.getUser().getId();
		log.info("내 팔로워 목록 조회 요청 - currentUserId: {}, page: {}, size: {}", currentUserId, page, size);

		Pageable pageable = PageRequest.of(page, size);
		SliceResponseDto<FollowListResponseDto> followers = followListService.getFollowers(currentUserId, currentUserId, pageable);
		return ApiResponse.onSuccess(SuccessCode.OK, followers);
	}

	// ===== 특정 사용자 팔로우 정보 조회 =====

	@Operation(summary = "특정 사용자의 팔로잉 목록 조회", description = "특정 사용자가 팔로우하는 사용자 목록을 조회합니다.")
	@GetMapping("/{userId}/followings")
	public ApiResponse<SliceResponseDto<FollowListResponseDto>> getUserFollowings(
		@Parameter(description = "조회할 사용자 ID", required = true)
		@PathVariable Long userId,
		@Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
		@RequestParam(defaultValue = "0") int page,
		@Parameter(description = "페이지 크기", example = "20")
		@RequestParam(defaultValue = "20") int size,
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {
		Long currentUserId = customUserDetails.getUser().getId();
		log.info("사용자 팔로잉 목록 조회 요청 - userId: {}, currentUserId: {}, page: {}, size: {}",
			userId, currentUserId, page, size);

		Pageable pageable = PageRequest.of(page, size);
		SliceResponseDto<FollowListResponseDto> followings = followListService.getFollowings(userId, currentUserId, pageable);
		return ApiResponse.onSuccess(SuccessCode.OK, followings);
	}

	@Operation(summary = "특정 사용자의 팔로워 목록 조회", description = "특정 사용자를 팔로우하는 사용자 목록을 조회합니다.")
	@GetMapping("/{userId}/followers")
	public ApiResponse<SliceResponseDto<FollowListResponseDto>> getUserFollowers(
		@Parameter(description = "조회할 사용자 ID", required = true)
		@PathVariable Long userId,
		@Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
		@RequestParam(defaultValue = "0") int page,
		@Parameter(description = "페이지 크기", example = "20")
		@RequestParam(defaultValue = "20") int size,
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {
		Long currentUserId = customUserDetails.getUser().getId();
		log.info("사용자 팔로워 목록 조회 요청 - userId: {}, currentUserId: {}, page: {}, size: {}",
			userId, currentUserId, page, size);

		Pageable pageable = PageRequest.of(page, size);
		SliceResponseDto<FollowListResponseDto> followers = followListService.getFollowers(userId, currentUserId, pageable);
		return ApiResponse.onSuccess(SuccessCode.OK, followers);
	}
}
