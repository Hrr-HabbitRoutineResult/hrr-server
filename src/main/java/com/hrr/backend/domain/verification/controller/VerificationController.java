package com.hrr.backend.domain.verification.controller;

import com.hrr.backend.domain.verification.dto.*;
import com.hrr.backend.domain.verification.service.VerificationService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Verification", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/verification")
public class VerificationController {

    private final VerificationService verificationService;

    /** 텍스트 인증 */
    @Operation(summary = "텍스트 인증", description = "글 인증으로 게시물을 작성합니다.")
    @PostMapping("/text")
    public ApiResponse<VerificationResponseDto> createTextVerification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long challengeId,
            @RequestParam(required = false) Long roundId,
            @Valid @RequestBody VerificationRequestDto request
    ) {
        Long userId = userDetails.getUser().getId();

        VerificationResponseDto response = verificationService.createTextVerification(
                challengeId, roundId, userId, request
        );

        return ApiResponse.onSuccess(SuccessCode.VERIFICATION_POST_OK, response);
    }

    /** 사진 인증 */
    @Operation(summary = "사진 인증", description = "사진 인증 게시물을 작성합니다.")
    @PostMapping(value = "/photo", consumes = "multipart/form-data")
    public ApiResponse<VerificationResponseDto> createPhotoVerification(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long challengeId,
            @RequestParam(required = false) Long roundId,
            @RequestPart("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam Boolean isQuestion
    ) {
        Long userId = userDetails.getUser().getId();

        // 1) 제목 검증
        if (title == null || title.trim().isEmpty()) {
            throw new GlobalException(ErrorCode.VERIFICATION_TITLE_REQUIRED);
        }

        // 2) 파일 존재 여부 확인
        if (file == null || file.isEmpty()) {
            throw new GlobalException(ErrorCode.VERIFICATION_FILE_REQUIRED);
        }

        // 3) 이미지 여부 확인
        if (file.getContentType() == null ||
                !file.getContentType().startsWith("image")) {
            throw new GlobalException(ErrorCode.VERIFICATION_FILE_NOT_IMAGE);
        }

        VerificationResponseDto response = verificationService.createPhotoVerification(
                challengeId, roundId, userId, file, title, isQuestion
        );

        return ApiResponse.onSuccess(SuccessCode.VERIFICATION_POST_OK, response);
    }

    // 사용자 본인 인증글 목록 조회
    @Operation(summary = "사용자 본인의 인증글 목록 조회", description = "본인이 작성한 인증글의 목록을 조회합니다.")
    @GetMapping("/me")
    public ApiResponse<VerificationMyResponseDto.MyPostList> getMyVerifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long challengeId,
            @RequestParam(required = false) Long roundId
    ) {
        Long userId = userDetails.getUser().getId();

        // createdAt 기준 최신순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        VerificationMyResponseDto.MyPostList result =
                verificationService.getMyVerifications(userId, challengeId, roundId, pageable);

        return ApiResponse.onSuccess(SuccessCode.OK, result);
    }

    /** 인증글 상세 조회 */
    @Operation(summary = "인증글 상세 조회", description = "verificationId로 인증글 1개를 조회합니다.")
    @GetMapping("/{verificationId}")
    public ApiResponse<VerificationDetailResponseDto> getVerificationDetail(
            @PathVariable Long verificationId
    ) {
        VerificationDetailResponseDto result =
                verificationService.getVerificationDetail(verificationId);

        return ApiResponse.onSuccess(SuccessCode.OK, result);
    }

    /** 챌린지 + 라운드별 인증글 전체 조회 */
    @Operation(
            summary = "챌린지랑 라운드별 인증글 조회",
            description = "특정 챌린지의 특정 라운드에 작성된 전체 인증글 목록을 조회합니다."
    )
    @GetMapping("/challenge/{challengeId}")
    public ApiResponse<VerificationListResponseDto.ListResponse> getVerificationsByChallengeAndRound(
            @PathVariable Long challengeId,
            @RequestParam(required = false) Long roundId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        // createdAt 기준 최신순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        VerificationListResponseDto.ListResponse result = verificationService.getVerificationsByChallengeAndRound(
                challengeId, roundId, pageable
        );

        return ApiResponse.onSuccess(SuccessCode.OK, result);
    }

}
