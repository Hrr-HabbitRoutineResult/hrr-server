package com.hrr.backend.domain.recommendation.controller;

import com.hrr.backend.domain.recommendation.dto.request.ChallengeRecommendRequest;
import com.hrr.backend.domain.recommendation.dto.response.ChallengeRecommendResult;
import com.hrr.backend.domain.recommendation.service.ChallengeRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/challenges/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final ChallengeRecommendationService recommendationService;

    @PostMapping
    public ResponseEntity<ChallengeRecommendResult> recommend(
            @RequestBody ChallengeRecommendRequest request
    ) {
        ChallengeRecommendResult result = recommendationService.recommendChallenges(request);
        return ResponseEntity.ok(result);
    }
}
