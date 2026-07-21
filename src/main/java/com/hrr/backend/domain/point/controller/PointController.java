package com.hrr.backend.domain.point.controller;

import com.hrr.backend.domain.point.dto.PointHistoryResponseDto;
import com.hrr.backend.domain.point.service.PointService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Point", description = "포인트 관련 API")
@RestController
@RequestMapping("/api/v1/point")
@RequiredArgsConstructor
@Validated
public class PointController {

    private final PointService pointService;

    @GetMapping("/history")
    @Operation(
            summary = "포인트 누적 현황 조회",
            description = "현재 로그인한 사용자의 최근 3개월(당월 포함) 포인트 적립 내역을 최신순으로 조회합니다."
    )
    public ApiResponse<SliceResponseDto<PointHistoryResponseDto.HistoryDto>> getMyPointHistory(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,

            @RequestParam(name = "page", defaultValue = "1")
            @Min(1)
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1") int page,

            @RequestParam(name = "size", defaultValue = "20")
            @Min(1) @Max(100)
            @Parameter(description = "페이지당 데이터 개수", example = "20") int size
    ) {
        SliceResponseDto<PointHistoryResponseDto.HistoryDto> response =
                pointService.getMyPointHistory(customUserDetails.getUser(), page - 1, size);

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}