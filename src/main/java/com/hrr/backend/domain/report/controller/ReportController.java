package com.hrr.backend.domain.report.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrr.backend.domain.report.dto.ReportRequestDto;
import com.hrr.backend.domain.report.service.ReportService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@Tag(name = "Report", description = "신고 관련 API")
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
@Validated
public class ReportController {

	private final ReportService reportService;

	@PostMapping("/verification/weak")
	@Operation(summary = "부실 인증 신고", description = "부실 인증을 신고합니다. \n중복 신고할 수 없으며, 해당 챌린지에 참가 중인 챌린저만 신고 가능합니다.")
	public ApiResponse<Void> reportWeakVerification(
		@Schema(description = "신고하려는 인증 ID; verificationID", example = "1")
		@NotNull(message = "신고 대상 ID는 필수입니다.")
		@RequestParam Long targetId,

		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserDetails userDetails
	){
		reportService.reportWeakVerification(userDetails.getUser(), targetId);

		return ApiResponse.onSuccess(SuccessCode.OK, null);
	}

	@PostMapping("/verification/post")
	@Operation(summary = "인증 게시글 신고", description = "인증 게시글을 신고합니다. 신고 누적 5회 시 해당 게시글에 접근할 수 없습니다.")
	public ApiResponse<Void> reportVerificationPost(
		@Valid @RequestBody ReportRequestDto request,

		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserDetails userDetails
	){
		reportService.reportVerificationPost(userDetails.getUser(), request);

		return ApiResponse.onSuccess(SuccessCode.OK, null);
	}

	@PostMapping("/user")
	@Operation(summary = "사용자 신고", description = "사용자를 신고합니다.")
	public ApiResponse<Void> reportUser(
		@Valid @RequestBody ReportRequestDto request,

		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserDetails userDetails
	){
		reportService.reportUser(userDetails.getUser(), request);

		return ApiResponse.onSuccess(SuccessCode.OK, null);
	}

}
