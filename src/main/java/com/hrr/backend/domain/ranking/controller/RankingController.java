package com.hrr.backend.domain.ranking.controller;

import com.hrr.backend.domain.ranking.dto.RankingResponseDto;
import com.hrr.backend.domain.ranking.service.RankingService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Ranking", description = "랭킹 관련 API")
@RestController
@RequestMapping("/api/v1/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    @Operation(
            summary = "랭킹 조회",
            description = "전체 사용자 대상 상위 5명 랭킹과 내 등수를 조회합니다. " +
                    "프로필/닉네임/현재 포인트는 실시간으로 반영되고, " +
                    "등수/상위 퍼센트/지난주 대비 변화/상위 5명 목록은 매주 월요일 00시 스냅샷 기준으로 고정됩니다."
    )
    public ApiResponse<RankingResponseDto.BoardDto> getMyRanking(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        RankingResponseDto.BoardDto response = rankingService.getMyRankingBoard(customUserDetails.getUser());
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}