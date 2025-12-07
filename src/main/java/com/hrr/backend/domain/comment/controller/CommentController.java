package com.hrr.backend.domain.comment.controller;

import com.hrr.backend.domain.comment.dto.CommentCreateRequestDto;
import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.comment.dto.CommentResponseDto;
import com.hrr.backend.domain.comment.dto.CommentUpdateRequestDto;
import com.hrr.backend.domain.comment.service.CommentService;
import com.hrr.backend.domain.auth.service.JwtService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comments")
@Tag(name = "Comment", description = "댓글/대댓글 API")
public class CommentController {

    private final CommentService commentService;
    private final JwtService jwtService;

    /** 댓글 작성 */
    @Operation(summary = "댓글 작성", description = "인증글에 댓글 또는 대댓글을 작성합니다.")
    @PostMapping("/{verificationId}")
    public ApiResponse<CommentResponseDto> createComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long verificationId,
            @Valid @RequestBody CommentCreateRequestDto requestDto
    ) {
        Long userId = userDetails.getUser().getId();

        CommentResponseDto response = commentService.createComment(verificationId, userId, requestDto);

        return ApiResponse.onSuccess(SuccessCode.COMMENT_POST_OK, response);
    }

    /** 댓글/대댓글 조회 */
    @Operation(summary = "댓글/대댓글 조회", description = "특정 인증글의 모든 댓글 및 대댓글을 조회합니다.")
    @GetMapping("/{verificationId}")
    public ApiResponse<CommentListResponseDto> getComments(
            @PathVariable Long verificationId,
            Pageable pageable
    ) {
        CommentListResponseDto response = commentService.getComments(verificationId, pageable);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    /** 댓글 수정 */
    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글만 수정할 수 있습니다.")
    @PatchMapping("/{commentId}")
    public ApiResponse<CommentResponseDto> updateComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId,
            @RequestBody CommentUpdateRequestDto requestDto
    ) {
        Long userId = userDetails.getUser().getId();

        CommentResponseDto response = commentService.updateComment(commentId, userId, requestDto);

        return ApiResponse.onSuccess(SuccessCode.COMMENT_POST_OK, response);

    }

    /** 댓글 삭제 */
    @Operation(summary = "댓글 삭제", description = "댓글을 Soft Delete 방식으로 삭제합니다.")
    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> deleteComment(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long commentId
    ) {
        Long userId = userDetails.getUser().getId();

        commentService.deleteComment(commentId, userId);

        return ApiResponse.onSuccess(SuccessCode.COMMENT_DELETE_OK, null);
    }
}
