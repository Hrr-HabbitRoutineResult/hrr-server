package com.hrr.backend.domain.verification.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class VerificationResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "인증 피드(목록) 항목 DTO")
    public static class FeedDto {

        @Schema(description = "인증 ID", example = "10")
        private Long verificationId;

        @Schema(description = "인증 유형 (CAMERA: 사진, TEXT: 글)", example = "TEXT")
        private VerificationPostType type;

        @Schema(description = "제목", example = "오늘의 질문입니다!")
        private String title;

        @Schema(description = "내용 (글 인증 미리보기용)", example = "이 부분 어떻게 해결하나요?")
        private String content;

        @Schema(description = "인증 사진 URL (사진 인증인 경우)", example = "https://example.com/photo.jpg")
        private String imageUrl;

        @Schema(description = "외부 링크 URL (있을 경우 링크 아이콘 표시)", example = "https://velog.io/my-post")
        private String linkUrl;

        @Schema(description = "질문글 여부 (Q 마크 표시)", example = "true")
        private Boolean isQuestion;

        @Schema(description = "질문 해결 여부 (채택 완료됨 - 정렬 후순위)", example = "false")
        private Boolean isResolved;

        @Schema(description = "작성일자", example = "2025.12.05")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
        private LocalDateTime createdDate;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "인증 통계 정보 DTO (헤더용)")
    public static class StatDto {

        @Schema(description = "집계된 인증 인원 수", example = "15")
        private Integer certifiedCount;

        @Schema(description = "전체 참가자 수", example = "30")
        private Integer totalParticipantCount;

        @Schema(description = "집계 기준 날짜", example = "2025-12-05")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
        private LocalDateTime baseDate;
    }

}