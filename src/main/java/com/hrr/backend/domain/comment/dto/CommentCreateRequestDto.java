package com.hrr.backend.domain.comment.dto;

import lombok.Getter;

@Getter
public class CommentCreateRequestDto {

    // null 이면 부모 댓글, 값이 있으면 해당 댓글의 대댓글
    private Long parentId;

    private boolean anonymous;

    private String content;
}
