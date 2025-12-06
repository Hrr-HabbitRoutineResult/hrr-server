package com.hrr.backend.domain.verification.controller;

import com.hrr.backend.domain.verification.dto.*;
import com.hrr.backend.domain.verification.service.VerificationService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Verification", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/verification")
public class VerificationController {

    private final VerificationService verificationService;

    /** TEXT 인증 생성 */
    @Operation(summary = "글 인증 생성")
    @PostMapping("/{challengeId}/text")
    public ApiResponse<VerificationResponseDto> createTextVerification(
            @Parameter(description = "챌린지 ID")
            @PathVariable Long challengeId,

            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody @Valid VerificationRequestDto request
    ) {
        Long userId = userDetails.getUser().getId();

        // roundId는 프론트에서 받지 않고, Service에서 현재 라운드를 자동 조회
        VerificationResponseDto response = verificationService.createTextVerification(
                challengeId, userId, request
        );

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    /** PHOTO 인증 생성 */
    @Operation(summary = "사진 인증 생성")
    @PostMapping("/{challengeId}/photo")
    public ApiResponse<VerificationResponseDto> createPhotoVerification(
            @Parameter(description = "챌린지 ID", required = true)
            @PathVariable Long challengeId,

            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody @Valid VerificationRequestPhotoDto request
    ) {
        Long userId = userDetails.getUser().getId();

        VerificationResponseDto response = verificationService.createPhotoVerification(
                challengeId,
                userId,
                request.getContent(),
                request.getS3Key(),
                request.getTitle(),
                request.getIsQuestion()
        );

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}
