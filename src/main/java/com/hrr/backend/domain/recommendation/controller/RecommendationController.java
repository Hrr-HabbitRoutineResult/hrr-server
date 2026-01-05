package com.hrr.backend.domain.recommendation.controller;

import com.hrr.backend.domain.recommendation.dto.request.ChallengeRecommendRequest;
import com.hrr.backend.domain.recommendation.dto.response.ChallengeRecommendResult;
import com.hrr.backend.domain.recommendation.service.ChallengeRecommendationService;
import com.hrr.backend.domain.user.service.UserFavorService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/challenges/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final ChallengeRecommendationService recommendationService;
	private final UserFavorService userFavorService;

    @PostMapping("")
    @Operation(summary = "챌린지 추천", description = "사용자 정보 기반으로 챌린지 추천을 반환합니다.")
    public ApiResponse<ChallengeRecommendResult> recommend(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChallengeRecommendRequest request
    ) {
        Long userId = userDetails.getUser().getId();
        request.setUserId(userId);
        ChallengeRecommendResult result = recommendationService.recommendChallenges(request);
        return ApiResponse.onSuccess(SuccessCode.OK, result);
    }
}
