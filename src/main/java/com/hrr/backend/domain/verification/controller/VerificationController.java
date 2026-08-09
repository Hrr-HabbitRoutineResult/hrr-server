package com.hrr.backend.domain.verification.controller;

import com.hrr.backend.domain.verification.dto.*;
import com.hrr.backend.domain.verification.service.VerificationService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Verification", description = "인증 관련 API")
@RestController
@RequestMapping("/api/v1/verifications") // 복수형으로 통일
@RequiredArgsConstructor
@Validated // @Min 동작을 위해 필요
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping("/{challengeId}/feed")
    @Operation(summary = "챌린지 내 인증 현황 피드 조회", description = "챌린지의 설정(글/사진)에 맞는 인증 목록을 조회합니다.")
    public ApiResponse<SliceResponseDto<VerificationResponseDto.FeedDto>> getVerificationFeed(
            @PathVariable(name = "challengeId") Long challengeId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "roundNumber") Integer roundNumber,
            @RequestParam(name = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Long currentUserId = userDetails.getUser().getId();

        SliceResponseDto<VerificationResponseDto.FeedDto> response =
                verificationService.getVerificationFeed(challengeId, roundNumber, currentUserId, page - 1, size);

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @GetMapping("/{challengeId}/stat")
    @Operation(summary = "챌린지 내 인증 통계 조회", description = "현재 진행 중인 라운드의 실시간(또는 최근) 인증 인원 수를 조회합니다.")
    public ApiResponse<VerificationResponseDto.StatDto> getVerificationStat(
            @PathVariable(name = "challengeId") Long challengeId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        VerificationResponseDto.StatDto response =
                verificationService.getVerificationStat(challengeId);

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @GetMapping("/{challengeId}/me")
    @Operation(summary = "챌린지 내 인증 현황 (마이)", description = "내 누적 인증 횟수, 경고 횟수 및 내가 작성한 인증글 목록을 조회합니다.")
    public ApiResponse<VerificationResponseDto.MyProfileDto> getMyVerificationProfile(
            @PathVariable(name = "challengeId") Long challengeId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "1") @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        VerificationResponseDto.MyProfileDto response = verificationService.getMyVerificationProfile(
                userDetails.getUser(), challengeId, page - 1, size
        );

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    /** TEXT 인증 생성 */
    @Operation(summary = "글 인증 생성")
    @PostMapping("/{challengeId}/text")
    public ApiResponse<VerificationResponseDto.CreateResponseDto> createTextVerification(
            @Parameter(description = "챌린지 ID")
            @PathVariable Long challengeId,

            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody @Valid VerificationRequestDto request
    ) {
        Long userId = userDetails.getUser().getId();

        VerificationResponseDto.CreateResponseDto response = verificationService.createTextVerification(
                challengeId, userId, request
        );

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    /** PHOTO 인증 생성 */
    @Operation(summary = "사진 인증 생성")
    @PostMapping("/{challengeId}/photo")
    public ApiResponse<VerificationResponseDto.CreateResponseDto> createPhotoVerification(
            @Parameter(description = "챌린지 ID", required = true)
            @PathVariable Long challengeId,

            @AuthenticationPrincipal CustomUserDetails userDetails,

            @RequestBody @Valid VerificationRequestPhotoDto request
    ) {
        Long userId = userDetails.getUser().getId();

        VerificationResponseDto.CreateResponseDto response = verificationService.createPhotoVerification(
                challengeId,
                userId,
                request.getContent(),
                request.getS3Key(),
                request.getTitle(),
                request.getIsQuestion()
        );

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @GetMapping("/{verificationId}")
    @Operation(summary = "인증 게시물 상세 조회", description = "인증 게시물 + 댓글 목록을 상세 조회합니다.")
    public ApiResponse<VerificationDetailResponseDto> getVerificationDetail(
            @PathVariable Long verificationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentUserId = (userDetails != null) ? userDetails.getUser().getId() : null;

        VerificationDetailResponseDto response =
                verificationService.getVerificationDetail(verificationId, currentUserId, page - 1, size);

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @PostMapping("/{verificationId}/comments/{commentId}/adopt")
    @Operation(summary = "댓글 채택하기", description = "인증글 작성자가 댓글을 채택합니다.")
    public ApiResponse<String> adoptComment(
            @PathVariable Long verificationId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentUserId = userDetails.getUser().getId();

        verificationService.adoptComment(verificationId, commentId, currentUserId);

        return ApiResponse.onSuccess(SuccessCode.COMMENT_ADOPT_OK, null);

    }

    @PatchMapping("/{verificationId}")
    @Operation(summary = "인증글 수정", description = "인증글을 수정합니다.")
    public ApiResponse<VerificationDetailResponseDto> updateVerification(
            @PathVariable Long verificationId,
            @RequestBody @Valid VerificationUpdateRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long currentUserId = userDetails.getUser().getId();
        VerificationDetailResponseDto dto =
                verificationService.updateVerification(verificationId, currentUserId, requestDto);

        return ApiResponse.onSuccess(SuccessCode.VERIFICATION_UPDATE_OK, dto);
    }

    @DeleteMapping("/{verificationId}")
    @Operation(summary = "인증글 삭제", description = "인증글을 삭제합니다.")
    public ApiResponse<String> deleteVerification(
            @PathVariable Long verificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        verificationService.deleteVerification(verificationId, userDetails.getUser().getId());

        return ApiResponse.onSuccess(SuccessCode.VERIFICATION_DELETE_OK, null);
    }

    @PutMapping("/{verificationId}/scrap")
    @Operation(summary = "인증 게시글 스크랩 등록", description = "로그인한 사용자가 인증 게시글을 스크랩합니다. 이미 스크랩한 게시글이면 중복 저장하지 않고 성공 처리합니다.")
    public ApiResponse<VerificationResponseDto.ScrapResponseDto> scrapVerification(
            @PathVariable Long verificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        VerificationResponseDto.ScrapResponseDto response =
                verificationService.scrapVerification(verificationId, userDetails.getUser());

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @DeleteMapping("/{verificationId}/scrap")
    @Operation(summary = "인증 게시글 스크랩 해제", description = "로그인한 사용자가 인증 게시글 스크랩을 해제합니다. 스크랩 데이터가 없어도 성공 처리합니다.")
    public ApiResponse<VerificationResponseDto.ScrapResponseDto> unscrapVerification(
            @PathVariable Long verificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        VerificationResponseDto.ScrapResponseDto response =
                verificationService.unscrapVerification(verificationId, userDetails.getUser());

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @PutMapping("/{verificationId}/likes")
    @Operation(summary = "인증 게시글 좋아요 등록", description = "로그인한 사용자가 인증 게시글에 좋아요를 등록합니다. 이미 좋아요한 게시글이면 중복 저장하지 않고 성공 처리합니다.")
    public ApiResponse<VerificationResponseDto.LikeResponseDto> likeVerification(
            @PathVariable Long verificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        VerificationResponseDto.LikeResponseDto response =
                verificationService.likeVerification(verificationId, userDetails.getUser());

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @DeleteMapping("/{verificationId}/likes")
    @Operation(summary = "인증 게시글 좋아요 취소", description = "로그인한 사용자가 인증 게시글 좋아요를 취소합니다. 좋아요 데이터가 없어도 성공 처리합니다.")
    public ApiResponse<VerificationResponseDto.LikeResponseDto> unlikeVerification(
            @PathVariable Long verificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        VerificationResponseDto.LikeResponseDto response =
                verificationService.unlikeVerification(verificationId, userDetails.getUser());

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }



}
