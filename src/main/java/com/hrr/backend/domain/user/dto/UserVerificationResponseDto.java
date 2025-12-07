package com.hrr.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserVerificationResponseDto {

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
}