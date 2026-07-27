package com.hrr.backend.domain.point.dto;

import java.time.LocalDateTime;

import com.hrr.backend.domain.point.entity.enums.PointType;

import com.hrr.backend.global.response.SliceResponseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class PointHistoryResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "포인트 누적 현황 조회 응답 DTO")
    public static class PageDto {

        @Schema(description = "현재 보유 포인트 (실시간)", example = "100")
        private Long totalPoints;

        @Schema(description = "포인트 적립 내역 (최근 3개월, 무한 스크롤)")
        private SliceResponseDto<HistoryDto> history;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "포인트 적립 내역 DTO")
    public static class HistoryDto {

        @Schema(description = "포인트 종류", example = "FIRST_VERIFICATION")
        private PointType pointType;

        @Schema(description = "포인트 종류 제목", example = "챌린지 첫 인증")
        private String title;

        @Schema(description = "상세 내용 (챌린지 제목 또는 랜덤미션 내용)", example = "26 하반기 취준")
        private String detail;

        @Schema(description = "적립 포인트", example = "1")
        private Integer points;

        @Schema(description = "적립 일시", example = "2026-07-21T10:00:00")
        private LocalDateTime createdAt;
    }
}