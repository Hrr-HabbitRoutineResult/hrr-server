package com.hrr.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.UserLevel;
import com.hrr.backend.domain.user.entity.enums.UserRole;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

public class UserResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "다른 사용자 프로필 정보 DTO")
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

        @Schema(description = "차단 여부", example = "false")
        private Boolean isBlocked;

        // Entity -> DTO 변환
        public static ProfileDto from(User user, Boolean isFollowing, Boolean isBlocked) {
            return ProfileDto.builder()
                    .userId(user.getId())
                    .nickname(user.getDisplayNickname())
                    .profileImage(user.getProfileImage())
                    .level(user.getUserLevel())
                    .followerCount(user.getFollowerCount())
                    .followingCount(user.getFollowingCount())
                    .isFollowing(isFollowing)
                    .isBlocked(isBlocked)
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

		@Setter
		@Schema(description = "인증 완료 여부", example = "false")
		private boolean isVerified;

        @JsonIgnore
        private LocalDateTime startDate;
    }
      
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "내 정보 조회 DTO")
    public static class MyInfoDto {

        @Schema(description = "사용자 ID", example = "12345")
        private Long userId;

        @Schema(description = "닉네임", example = "흐르르")
        private String nickname;

        @Schema(description = "이메일", example = "my_email@example.com")
        private String email;

        @Schema(description = "전화번호", example = "010-1234-5678")
        private String phoneNumber;

        @Schema(description = "프로필 사진 URL", example = "https://example.com/my_photo.jpg")
        private String profileImage;

        @Schema(description = "레벨", example = "BRONZE")
        private UserLevel level;

        @Schema(description = "팔로워 수", example = "500")
        private Long followerCount;

        @Schema(description = "팔로잉 수", example = "150")
        private Long followingCount;

        @Schema(description = "포인트", example = "10000")
        private Long points;

        @Schema(description = "공개 여부", example = "true")
        private Boolean isPublic;

        @Schema(description = "역할", example = "USER")
        private UserRole role;

        @Schema(description = "상태", example = "ACTIVE")
        private UserStatus status;

        @Schema(description = "알림 설정 ID", example = "1")
        private Long alarmId;

        @Schema(description = "생성일시", example = "2025-01-01T10:00:00Z")
        private LocalDateTime createdAt;

        @Schema(description = "수정일시", example = "2025-10-09T15:30:00Z")
        private LocalDateTime updatedAt;

        public static MyInfoDto from(User user) {
            return MyInfoDto.builder()
                    .userId(user.getId())
                    .nickname(user.getDisplayNickname())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .profileImage(user.getProfileImage())
                    .level(user.getUserLevel())
                    .followerCount(user.getFollowerCount())
                    .followingCount(user.getFollowingCount())
                    .points(user.getPoints())
                    .isPublic(user.getIsPublic())
                    .role(user.getUserRole())
                    .status(user.getUserStatus())
                    .alarmId(null) // NotificationSetting에서 가져와야 함. 임시
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
        }
    }
}
