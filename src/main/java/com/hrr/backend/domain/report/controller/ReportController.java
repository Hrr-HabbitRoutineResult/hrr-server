package com.hrr.backend.domain.report.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrr.backend.domain.report.dto.ReportRequestDto;
import com.hrr.backend.domain.report.service.ReportService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Report", description = "신고 관련 API")
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
@Validated
public class ReportController {

	private final ReportService reportService;

	@PostMapping("/verification/post")
	@Operation(summary = "인증 게시글 신고", description = "인증 게시글을 신고합니다. 신고 누적 5회 시 해당 게시글에 접근할 수 없습니다.")
	public ApiResponse<Void> reportVerificationPost(
		@Valid @RequestBody ReportRequestDto request,

		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserDetails userDetails
	){
		reportService.reportVerificationPost(userDetails.getUser().getId(), request);

		return ApiResponse.onSuccess(SuccessCode.OK, null);
	}

	@PostMapping("/user")
	@Operation(summary = "사용자 신고", description = "사용자를 신고합니다.")
	public ApiResponse<Void> reportUser(
		@Valid @RequestBody ReportRequestDto request,

		@Parameter(hidden = true)
		@AuthenticationPrincipal CustomUserDetails userDetails
	){
		reportService.reportUser(userDetails.getUser().getId(), request);

		return ApiResponse.onSuccess(SuccessCode.OK, null);
	}
}
