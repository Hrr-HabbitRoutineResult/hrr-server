package com.hrr.backend.domain.comment.converter;

import com.hrr.backend.domain.comment.dto.CommentResponseDto;
import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.user.entity.User;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CommentConverter {

    public static CommentResponseDto toDto(Comment comment) {

        User user = comment.getUser();

        return CommentResponseDto.builder()
                .commentId(comment.getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .verificationId(comment.getVerification().getId())

                // 작성자 ID
                .userId(user.getId())

                // 닉네임 반환
                .userName(user.getNickname())

                // profileImage (URL)
                .userProfileUrl(user.getProfileImage())

                .isAnonymous(comment.isAnonymous())
                .depth(comment.getDepth())
                .content(comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .likesCount(comment.getLikesCount())
                .isAdopted(Boolean.TRUE.equals(comment.getIsAdopted()))
                .createdAt(comment.getCreatedAt())
				.updatedAt(comment.getUpdatedAt())
                .build();
    }
}
