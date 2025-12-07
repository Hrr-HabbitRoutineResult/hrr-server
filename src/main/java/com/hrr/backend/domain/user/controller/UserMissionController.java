package com.hrr.backend.domain.user.controller;

import java.time.LocalDate;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrr.backend.domain.user.dto.UserMissionRequestDto;
import com.hrr.backend.domain.user.dto.UserMissionResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.service.UserMissionService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
	public ApiResponse<UserMissionResponseDto.DetailDto> getRandomMission(
		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {
		User user = customUserDetails.getUser();

		return ApiResponse.onSuccess(SuccessCode.OK, userMissionService.getRandomMission(user));
	}

	@GetMapping("/completed")
	@Operation(summary = "오늘의 랜덤미션 완료 여부 조회", description = "오늘의 랜덤미션을 완료하였는지 여부를 조회합니다.")
	public ApiResponse<Boolean> getRandomMissionStatus(
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {
		User user = customUserDetails.getUser();

		return ApiResponse.onSuccess(SuccessCode.OK, userMissionService.getRandomMissionStatus(user));
	}

	@PostMapping("/verify")
	@Operation(summary = "오늘의 랜덤미션 인증", description = "오늘의 랜덤미션을 인증합니다. ")
	public ApiResponse<String> getRandomMission(
		UserMissionRequestDto.VerificationDto request,

		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserDetails customUserDetails
	) {

		LocalDate today = LocalDate.now();

		userMissionService.verifyRandomMission(customUserDetails.getUser(), request.getMissionId(), today, request.getImageKey());

		return ApiResponse.onSuccess(SuccessCode.OK, "랜덤미션 인증에 성공하였습니다.");
	}
}
