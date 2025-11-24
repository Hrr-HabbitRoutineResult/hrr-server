package com.hrr.backend.domain.challenge.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.VerificationType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ChallengeRequestDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "챌린지 개설 요청 DTO")
    public static class CreateChallengeDto {

        @Schema(description = "챌린지 제목", example = "매일 1만 보 걷기")
        @NotBlank(message = "title은 필수입니다.")
        @Size(max = 10, message = "title은 10자를 초과할 수 없습니다.")
        private String title;

        @Schema(description = "챌린지 설명", example = "하루에 만 보 이상 걷는 습관")
        @NotBlank(message = "description은 필수입니다.")
        @Size(max = 20, message = "description은 20자를 초과할 수 없습니다.")
        private String description;

        @Schema(description = "공개 여부 (true: 공개, false: 비공개)", example = "true")
        @NotNull(message = "isPublic은 필수입니다.")
        private Boolean isPublic;

        @Schema(description = "비공개 비밀번호 (4자리 숫자). 공개 챌린지라면 null 가능", example = "1234")
        @Pattern(regexp = "^\\d{4}$", message = "비밀번호는 4자리 숫자여야 합니다.")
        private String password;

        @Schema(description = "챌린지 카테고리", example = "HEALTH")
        @NotNull(message = "category는 필수입니다.")
        private Category category;

        @Schema(description = "인증 방식", example = "PHOTO")
        @NotNull(message = "verificationType은 필수입니다.")
        private VerificationType verificationType;

        @Schema(description = "시작 시간", example = "2025-11-24T10:00")
        @NotNull(message = "startDate는 필수입니다.")
        private LocalDateTime startDate;

        @Schema(description = "최대 참여 인원 (1명 이상)", example = "10")
        @NotNull(message = "maxParticipants는 필수입니다.")
        @Min(value = 1, message = "maxParticipants는 1 이상이어야 합니다.")
        @Max(value = 30, message = "maxParticipants는 30 이하여야 합니다.")
        private Integer maxParticipants;

        @Schema(description = "관찰자 모드 허용 여부", example = "true")
        @NotNull(message = "isViewerMode는 필수입니다.")
        private Boolean isViewerMode;

        @Schema(description = "챌린지 규칙", example = "하루에 1만 보 이상 걸은 스크린샷을 인증해야 합니다.")
        @Size(max = 200, message = "rule은 200자를 초과할 수 없습니다.")
        private String rule;

        @Schema(description = "인증 시작 시간", example = "06:00:00")
        @NotNull(message = "verifyStartTime은 필수입니다.")
        private LocalTime verifyStartTime;

        @Schema(description = "인증 종료 시간", example = "23:00:00")
        @NotNull(message = "verifyEndTime은 필수입니다.")
        private LocalTime verifyEndTime;

        @Schema(description = "인증 요일 목록", example = "[\"MONDAY\", \"WEDNESDAY\", \"FRIDAY\"]")
        @NotEmpty(message = "daysOfWeek는 최소 1개 이상이어야 합니다.")
        private List<ChallengeDays> daysOfWeek;

        @Schema(description = "챌린지 이미지 URL (없으면 서버에서 기본 이미지 사용)", example = "https://example.com/images/challenge-default.png")
        private String imageUrl;
    }
}
