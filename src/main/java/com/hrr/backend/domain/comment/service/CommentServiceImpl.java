package com.hrr.backend.domain.comment.service;

import com.hrr.backend.domain.comment.converter.CommentConverter;
import com.hrr.backend.domain.comment.dto.CommentCreateRequestDto;
import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.comment.dto.CommentResponseDto;
import com.hrr.backend.domain.comment.dto.CommentUpdateRequestDto;
import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.comment.repository.CommentRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final VerificationRepository verificationRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    /** 댓글 작성 */
    @Override
    @Transactional
    public CommentResponseDto createComment(Long verificationId, Long userId, CommentCreateRequestDto requestDto) {

        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        Comment parent = null;

        // parentId 가 있으면 대댓글
        if (requestDto.getParentId() != null) {
            parent = commentRepository.findById(requestDto.getParentId())
                    .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_INVALID_PARENT));

            //부모 댓글의 dpth가 1이상 존재해서 대댓글인 경우에 더 이상 답글을 달 수 없도록 진행
            if (parent.getDepth() >= 1) {
                throw new GlobalException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
            }

            // 부모 댓글의 인증글과 현재 인증글이 다르면 에러
            if (!parent.getVerification().getId().equals(verificationId)) {
                throw new GlobalException(ErrorCode.COMMENT_INVALID_PARENT);
            }
        }

        Comment comment = Comment.create(
                verification,
                user,
                parent,
                requestDto.getContent(),
                requestDto.isAnonymous(),
                parent == null ? 0 : parent.getDepth() + 1
        );

        commentRepository.save(comment);

        return CommentConverter.toDto(comment);
    }

    /** 댓글/대댓글 조회 */
    @Override
    public CommentListResponseDto getComments(Long verificationId) {

        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        // [수정] 부모 댓글 조회 시 '삭제되지 않은 것(IsDeletedFalse)'만 조회
        List<Comment> parents =
                commentRepository.findByVerificationAndDepthAndIsDeletedFalseOrderByCreatedAtAsc(verification, 0);

        List<CommentResponseDto> result = new ArrayList<>();

        for (Comment parent : parents) {
            // 부모 댓글
            result.add(CommentConverter.toDto(parent));

            // 자식 댓글(대댓글) 조회 시에도 삭제되지 않은 것만 조회
            List<Comment> children = commentRepository.findByParentAndIsDeletedFalseOrderByCreatedAtAsc(parent);

            for (Comment child : children) {
                result.add(CommentConverter.toDto(child));
            }
        }

        return new CommentListResponseDto(result.size(), result);
    }

    /** 댓글 수정 */
    @Override
    @Transactional
    public CommentResponseDto updateComment(Long commentId, Long userId, CommentUpdateRequestDto requestDto) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.COMMENT_UNAUTHORIZED);
        }

        comment.updateContent(requestDto.getContent());

        return CommentConverter.toDto(comment);
    }


    /** 댓글 삭제 (Soft Delete) */
    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.COMMENT_UNAUTHORIZED);
        }
// 엔티티에 @SQLDelete가 적용되어 있으므로 repository.delete를 호출하면 update 쿼리가 실행O
        commentRepository.delete(comment);
    }
}
