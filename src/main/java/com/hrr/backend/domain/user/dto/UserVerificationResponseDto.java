package com.hrr.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public class UserVerificationResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인증 기록 전체 응답 DTO")
    public static class VerificationHistoryDto {

        @Schema(description = "인증 기록 목록")
        private List<VerificationItemDto> verificationList;

        @Schema(description = "페이지네이션 정보")
        private PaginationDto pagination;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "개별 인증 기록 DTO")
    public static class VerificationItemDto {

        @Schema(description = "인증 ID", example = "1")
        private Long verificationId;

        @Schema(description = "챌린지 ID", example = "101")
        private Long challengeId;

        @Schema(description = "챌린지 제목", example = "미라클 모닝")
        private String challengeTitle;

        @Schema(description = "인증 제목", example = "해피뉴이어! 올해 마지막 인증 올립니다")
        private String title;

        @Schema(description = "인증 내용", example = "여기엔 상세내용이 들어가유~")
        private String content;

        @Schema(description = "이미지 URL", example = "https://example.com/verification_image_1.jpg")
        private String imageUrl;

        @Schema(description = "인증 일시", example = "2025-09-18T08:00:00Z")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        private LocalDateTime verifiedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "페이지네이션 정보 DTO")
    public static class PaginationDto {

        @Schema(description = "전체 데이터 수", example = "120")
        private Long totalCount;

        @Schema(description = "현재 페이지", example = "1")
        private Integer currentPage;

        @Schema(description = "전체 페이지 수", example = "12")
        private Integer totalPages;

        @Schema(description = "페이지 크기", example = "10")
        private Integer pageSize;

        public static PaginationDto from(Page<?> page) {
            return PaginationDto.builder()
                    .totalCount(page.getTotalElements())
                    .currentPage(page.getNumber() + 1) // 0-based -> 1-based
                    .totalPages(page.getTotalPages())
                    .pageSize(page.getSize())
                    .build();
        }
    }
}