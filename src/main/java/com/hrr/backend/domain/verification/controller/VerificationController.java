package com.hrr.backend.domain.verification.controller;

import com.hrr.backend.domain.verification.dto.VerificationDetailResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationMyResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationRequestDto;
import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.domain.verification.service.VerificationService;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Verification", description = "인증 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/verification")
public class VerificationController {

    private final VerificationService verificationService;

    /** 텍스트 인증 */
    @Operation(summary = "텍스트 인증", description = "글 인증으로 게시물을 작성합니다.")
    @PostMapping("/text")
    public ResponseEntity<VerificationResponseDto> createTextVerification(
            @RequestParam Long userId,
            @RequestParam Long challengeId,
            @RequestParam(required = false) Long roundId,
            @RequestBody VerificationRequestDto request
    ) {

        // 1) 제목 필수 검증
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new GlobalException(ErrorCode.VERIFICATION_TITLE_REQUIRED);
        }

        // 2) content 필수 검증
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new GlobalException(ErrorCode.VERIFICATION_TEXT_REQUIRED);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(verificationService.createTextVerification(
                        challengeId, roundId, userId, request
                ));
    }

    /** 사진 인증 */
    @Operation(summary = "사진 인증", description = "사진 인증 게시물을 작성합니다.")
    @PostMapping(value = "/photo", consumes = "multipart/form-data")
    public ResponseEntity<VerificationResponseDto> createPhotoVerification(
            @RequestParam Long userId,
            @RequestParam Long challengeId,
            @RequestParam(required = false) Long roundId,
            @RequestPart("file") MultipartFile file,
            @RequestParam String title,
            @RequestParam Boolean isQuestion
    ) {

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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(verificationService.createPhotoVerification(
                        challengeId, roundId, userId, file, title, isQuestion
                ));
    }
// 사용자 본인 인증글 목록 조회
@Operation(summary = "사용자 본인의 인증글 목록 조회", description = "본인이 작성한 인증글의 목록을 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<VerificationMyResponseDto.MyPostList> getMyVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long challengeId,
            @RequestParam(required = false) Long roundId,
            HttpServletRequest request
    ) {
        String accessToken = request.getHeader("Authorization");

        if (accessToken == null || accessToken.isBlank()) {
            throw new GlobalException(ErrorCode.AUTH_TOKEN_MISSING);
        }
        // createdAt 기준 최신순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        VerificationMyResponseDto.MyPostList result =
                verificationService.getMyVerifications(accessToken, challengeId, roundId, pageable);

        return ResponseEntity.ok(result);
    }
    /** 인증글 상세 조회 */
    @Operation(summary = "인증글 상세 조회", description = "verificationId로 인증글 1개를 조회합니다.")
    @GetMapping("/{verificationId}")
    public ResponseEntity<VerificationDetailResponseDto> getVerificationDetail(
            @PathVariable Long verificationId
    ) {
        VerificationDetailResponseDto result =
                verificationService.getVerificationDetail(verificationId);

        return ResponseEntity.ok(result);
    }

    /** 챌린지 + 라운드별 인증글 전체 조회 */
    @Operation(
            summary = "챌린지랑 라운드별 인증글 조회",
            description = "특정 챌린지의 특정 라운드에 작성된 전체 인증글 목록을 조회합니다."
    )
    //@GetMapping("/challenge/{challengeId}/round/{roundId}")
    @GetMapping("/challenge/{challengeId}")
    public ResponseEntity<?> getVerificationsByChallengeAndRound(
            @PathVariable Long challengeId,
            //@PathVariable Long roundId,
            @RequestParam(required = false) Long roundId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        // createdAt 기준 최신순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(
                verificationService.getVerificationsByChallengeAndRound(
                        challengeId,
                        roundId,
                        pageable
                )
        );
    }

}
