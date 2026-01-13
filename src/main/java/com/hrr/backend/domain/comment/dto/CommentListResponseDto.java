package com.hrr.backend.domain.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CommentListResponseDto {

    private CommentResponseDto adoptedParent;
    private List<CommentResponseDto> adoptedChildren;

    private List<CommentResponseDto> comments; // 데이터 리스트

    // 페이지네이션 정보 (부모 댓글 기준)
    private int currentPage;
    private int totalPages;
    private long totalParentElements; // 전체 부모 댓글 수
    private long totalCount; //전체 댓글 수 (부모 + 자식)
    private int size;
    private boolean isFirst;
    private boolean isLast;
}
