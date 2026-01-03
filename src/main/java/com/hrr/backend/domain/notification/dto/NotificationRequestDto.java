package com.hrr.backend.domain.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class NotificationRequestDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "알림 설정 수정 요청 DTO")
    public static class UpdateSettingDto {
        @Schema(description = "전체 일시 중단 조작 (true: 전체 끄기, false: 전체 켜기)", example = "false")
        private Boolean isAllPaused;

        @Schema(description = "챌린지 알림 설정", example = "true")
        private Boolean isChallengeEnabled;

        @Schema(description = "인증 알림 설정", example = "true")
        private Boolean isVerificationEnabled;

        @Schema(description = "팔로우 알림 설정", example = "true")
        private Boolean isFollowEnabled;

        @Schema(description = "배지 알림 설정", example = "true")
        private Boolean isBadgeEnabled;
    }
}