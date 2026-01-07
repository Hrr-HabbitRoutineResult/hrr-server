package com.hrr.backend.domain.round.controller;

import com.hrr.backend.domain.round.dto.RoundDecisionResponseDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.hrr.backend.domain.round.dto.RoundDecisionRequestDto;
import com.hrr.backend.domain.round.service.RoundDecisionService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/challenges")
@Validated
public class RoundController {

    private final RoundDecisionService roundDecisionService;

    // POST /api/v1/challenges/{challengeId}/rounds/decision
    @PostMapping("/{challengeId}/rounds/decision")
    public ApiResponse<RoundDecisionResponseDto> decideNextRound(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long challengeId,
            @RequestBody @Valid RoundDecisionRequestDto request
    ) {
        RoundDecisionResponseDto response = roundDecisionService.decideNextRound(
                userDetails.getUser().getId(),
                challengeId,
                request
        );
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}
