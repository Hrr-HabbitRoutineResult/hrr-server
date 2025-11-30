package com.hrr.backend.domain.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CommentListResponseDto {

    private int totalCount;
    private List<CommentResponseDto> comments;
}
