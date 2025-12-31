package com.hrr.backend.domain.comment.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponseDto {

    private Long commentId;
    private Long parentId;

    // 어떤 인증글(게시글)에 달린 댓글인지
    private Long verificationId;

    private Long userId;
    private String userName;
    private String userProfileUrl;

    private boolean isAnonymous;
    private int depth;

    private boolean isAdopted;

    private String content;
    private int likesCount;

	private boolean isMyComment; // 조회하는 사람이 쓴 댓글인지(프론트 분기 처리용)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
