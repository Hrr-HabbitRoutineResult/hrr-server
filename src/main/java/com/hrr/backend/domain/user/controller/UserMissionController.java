package com.hrr.backend.domain.user.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.service.UserMissionService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Mission", description = "랜덤미션 관련 API")
@RestController
@RequestMapping("/api/v1/users/mission/daily")
@RequiredArgsConstructor
public class UserMissionController {

	private final UserMissionService userMissionService;

	@GetMapping("")
	@Operation(summary = "오늘의 랜덤미션 조회", description = "오늘의 랜덤미션을 조회합니다. ")
	public ApiResponse<SliceResponseDto<ChallengeResponseDto.InfoDto>> getRandomMission(
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {

		return null;
	}

	@GetMapping("/completed")
	@Operation(summary = "오늘의 랜덤미션 완료 여부 조회", description = "오늘의 랜덤미션을 완료하였는지 여부를 조회합니다.")
	public ApiResponse<Boolean> getRandomMissionStatus(
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {
		User user = customUserDetails.getUser();

		return ApiResponse.onSuccess(SuccessCode.OK, userMissionService.getRandomMissionStatus(user));
	}
}
