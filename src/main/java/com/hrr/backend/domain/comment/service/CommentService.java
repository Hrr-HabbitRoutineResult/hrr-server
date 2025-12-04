package com.hrr.backend.domain.comment.service;

import com.hrr.backend.domain.comment.dto.CommentCreateRequestDto;
import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.comment.dto.CommentResponseDto;
import com.hrr.backend.domain.comment.dto.CommentUpdateRequestDto;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    /** 댓글 작성 */
    CommentResponseDto createComment(Long verificationId, Long userId, CommentCreateRequestDto requestDto);

    /** 댓글 조회 */
    CommentListResponseDto getComments(Long verificationId, Pageable pageable);

    /** 댓글 수정 */
    CommentResponseDto updateComment(Long commentId, Long userId, CommentUpdateRequestDto requestDto);

    /** 댓글 삭제 */
    void deleteComment(Long commentId, Long userId);
}
