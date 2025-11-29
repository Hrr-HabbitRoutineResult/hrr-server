package com.hrr.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.UserLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

public class UserResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "사용자 프로필 정보 DTO")
    public static class ProfileDto {

        @Schema(description = "사용자 아이디", example = "999")
        private Long userId;

        @Schema(description = "닉네임", example = "흐르르")
        private String nickname;

        @Schema(description = "프로필 사진 URL", example = "https://example.com/bora.jpg")
        private String profileImage;

        @Schema(description = "사용자 레벨", example = "BRONZE")
        private UserLevel level;

        @Schema(description = "팔로워 수", example = "50")
        private Long followerCount;

        @Schema(description = "팔로잉 수", example = "30")
        private Long followingCount;

        @Schema(description = "팔로잉 여부", example = "false")
        private Boolean isFollowing;

        // Entity -> DTO 변환
        public static ProfileDto from(User user, Boolean isFollowing) {
            return ProfileDto.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .profileImage(user.getProfileImage())
                    .level(user.getUserLevel())
                    .followerCount(user.getFollowerCount())
                    .followingCount(user.getFollowingCount())
                    .isFollowing(isFollowing)
                    .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "참가중인 챌린지 정보 DTO")
    public static class OngoingChallengeDto {

        @Schema(description = "챌린지 아이디", example = "301")
        private Long challengeId;

        @Schema(description = "챌린지 제목", example = "자잘자잘")
        private String title;

        @Schema(description = "챌린지 간단 설명", example = "하루 5분씩 무엇이든 꼭 해야...")
        private String description;

        @JsonProperty("image")
        @Schema(description = "챌린지 대표 이미지 URL", example = "http://example.com/challenge_301.jpg")
        private String thumbnailUrl;

        @Schema(description = "현재 회차 (인증 성공 횟수)", example = "6")
        private Integer currentRound;

        @JsonIgnore
        private LocalDateTime startDate;
    }
}