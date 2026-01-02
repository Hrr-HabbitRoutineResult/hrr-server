package com.hrr.backend.domain.user.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrr.backend.domain.user.dto.UserBlockResponse;
import com.hrr.backend.domain.user.service.UserBlockService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "UserBlock", description ="차단 관련 API")
@RestController
@RequestMapping("/api/v1/blocks")
@RequiredArgsConstructor
@Validated
public class UserBlockController {
	private final UserBlockService userBlockService;

	// 사용자 차단하기
	@Operation(
		summary = "사용자 차단",
		description = "로그인한 사용자 기준, 특정 사용자를 차단합니다. 차단 시 팔로우/팔로워가 해제 됩니다."
	)
	@PostMapping("/{blockedId}")
	public ApiResponse<String> block(
		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserDetails customUserDetails,

		@PathVariable Long blockedId
	) {
		userBlockService.blockUser(customUserDetails.getUser().getId(), blockedId);
		return ApiResponse.onSuccess(SuccessCode.OK, "차단에 성공하였습니다.");
	}

	// 차단 해제하기
	@Operation(
		summary = "사용자 차단 해제",
		description = "특정 사용자에 대한 차단을 해제합니다. 팔로우는 복구되지 않습니다."
	)
	@DeleteMapping("/{blockedId}")
	public ApiResponse<String> unblock(
		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserDetails customUserDetails,

		@PathVariable Long blockedId
	) {
		userBlockService.unblock(customUserDetails.getUser().getId(), blockedId);
		return ApiResponse.onSuccess(SuccessCode.OK, "차단 해제에 성공하였습니다.");
	}

	// 나의 차단 목록 조회
	@Operation(
		summary = "내 차단 목록 조회",
		description = "내가 차단한 사용자들의 목록을 조회합니다."
	)
	@GetMapping("/me")
	public ApiResponse<SliceResponseDto<UserBlockResponse>> getMyBlockList(
		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserDetails customUserDetails,

		@RequestParam(name = "page", defaultValue = "1")
		@Min(1)
		@Parameter(description = "페이지 번호 (1부터 시작)", example = "1") int page,

		@RequestParam(name = "size", defaultValue = "10")
		@Min(1) @Max(100)
		@Parameter(description = "페이지당 데이터 개수", example = "10") int size
	) {

		// 1-based로 변환하여 전달
		Pageable pageable = PageRequest.of(page - 1, size);

		return ApiResponse.onSuccess(SuccessCode.OK, userBlockService.getMyBlockList(customUserDetails.getUser(), pageable));
	}
}
