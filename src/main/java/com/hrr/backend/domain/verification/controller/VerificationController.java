package com.hrr.backend.domain.verification.controller;

import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.domain.verification.service.VerificationService;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Verification", description = "인증 관련 API")
@RestController
@RequestMapping("/api/v1/verifications")
@RequiredArgsConstructor
@Validated
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping("")
    @Operation(summary = "챌린지 인증 피드 조회", description = "챌린지의 설정(글/사진)에 맞는 인증 목록을 조회합니다.")
    public ApiResponse<SliceResponseDto<VerificationResponseDto.FeedDto>> getVerificationFeed(
            @RequestParam(name = "challengeId") Long challengeId,
            @RequestParam(name = "roundNumber") Integer roundNumber,
            @RequestParam(name = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        SliceResponseDto<VerificationResponseDto.FeedDto> response =
                verificationService.getVerificationFeed(challengeId, roundNumber, page - 1, size);

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @GetMapping("/stat")
    @Operation(summary = "인증 인원 통계 조회", description = "현재 진행 중인 라운드의 실시간(또는 최근) 인증 인원 수를 조회합니다.")
    public ApiResponse<VerificationResponseDto.StatDto> getVerificationStat(
            @RequestParam(name = "challengeId") Long challengeId
    ) {
        VerificationResponseDto.StatDto response =
                verificationService.getVerificationStat(challengeId);

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

}