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

                // 익명이면 "익명", 아니면 nickname
                .userName(comment.isAnonymous() ? "익명" : user.getNickname())

                // 익명이면 null, 아니면 profileImage (URL)
                .userProfileUrl(comment.isAnonymous() ? null : user.getProfileImage())

                .isAnonymous(comment.isAnonymous())
                .depth(comment.getDepth())
                .content(comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent())
                .likesCount(comment.getLikesCount())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
