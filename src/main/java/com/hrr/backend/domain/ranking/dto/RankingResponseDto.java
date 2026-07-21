package com.hrr.backend.domain.ranking.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class RankingResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "랭킹 조회 응답 DTO")
    public static class BoardDto {

        @Schema(description = "내 프로필 정보 (실시간 반영)")
        private MyProfileDto myProfile;

        @Schema(description = "랭킹 보드 정보 (매주 월요일 00시 스냅샷 기준)")
        private BoardInfoDto board;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "내 프로필 정보 DTO (실시간)")
    public static class MyProfileDto {

        @Schema(description = "닉네임", example = "김흐르")
        private String nickname;

        @Schema(description = "프로필 사진 URL", example = "https://example.com/profile.jpg")
        private String profileImage;

        @Schema(description = "현재 포인트 (실시간)", example = "100")
        private Long points;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "랭킹 보드 DTO (주간 스냅샷 기준)")
    public static class BoardInfoDto {

        @Schema(description = "내 등수 (스냅샷 기준)", example = "30")
        private Integer myRank;

        @Schema(description = "스냅샷 기준 전체 인원수", example = "100")
        private Integer totalUserCount;

        @Schema(description = "상위 퍼센트 (본인 포함, 올림 처리)", example = "30")
        private Integer topPercent;

        @Schema(description = "직전 스냅샷 대비 등수 변화값. 양수=상승한 계단 수, 음수=하락한 계단 수, 0=변동없음, null=비교할 이전 데이터 없음(첫 주)", example = "5")
        private Integer rankChange;

        @Schema(description = "등수 변화 문구. 이전 데이터가 없으면 null (이 경우 프론트에서 관련 문구 자체를 노출하지 않아야 함)", example = "지난주보다 5계단 상승했어요")
        private String rankChangeMessage;

        @Schema(description = "내 포인트 (스냅샷 기준)", example = "100")
        private Long myPoints;

        @Schema(description = "상위 5명 랭킹 목록")
        private List<RankingEntryDto> topRankers;

        @Schema(description = "랭킹보드 스냅샷 기준일 (매주 월요일)", example = "2026-07-20")
        private LocalDate snapshotDate;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "랭킹 항목 DTO")
    public static class RankingEntryDto {

        @Schema(description = "사용자 ID", example = "999")
        private Long userId;

        @Schema(description = "등수", example = "1")
        private Integer rank;

        @Schema(description = "닉네임", example = "라인")
        private String nickname;

        @Schema(description = "프로필 사진 URL", example = "https://example.com/profile.jpg")
        private String profileImage;

        @Schema(description = "포인트", example = "2700")
        private Long points;
    }
}