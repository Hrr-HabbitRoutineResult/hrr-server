package com.hrr.backend.domain.term.dto;

import lombok.Builder;

public class TermResponseDto {

    @Builder
    public record TermSummary(
            Long id,
            String title,
            Boolean isRequired
    ) {}

    @Builder
    public record TermDetail(
            Long id,
            String title,
            String description,
            Boolean isRequired
    ) {}
}
