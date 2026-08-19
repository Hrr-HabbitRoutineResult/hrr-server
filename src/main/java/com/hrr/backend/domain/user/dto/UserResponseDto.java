package com.hrr.backend.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.UserLevel;
import com.hrr.backend.domain.user.entity.enums.UserRole;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
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
                    // 내가 차단한 유저라면 사진을 null로 마스킹
                    .profileImage(isBlocked ? null : user.getProfileImage())
                    .level(user.getUserLevel())
                    // 탈퇴 유저는 0, 그 외(차단 포함)는 실제 카운트 유지
                    .followerCount(user.isNotActive() ? 0L : user.getFollowerCount())
                    .followingCount(user.isNotActive() ? 0L : user.getFollowingCount())
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

        @Schema(description = "챌린지 시작 여부", example = "true")
        private Boolean isStarted;

        @Schema(description = "현재 회차 (시작 전이면 null)", example = "3")
        private Integer currentRound;

        @Schema(description = "시작까지 남은 일수 (시작했으면 null)", example = "5")
        private Integer dday;

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
                    .createdAt(user.getCreatedAt())
                    .updatedAt(user.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "찜한 챌린지 정보 DTO")
    public static class LikedChallengeDto {

        @Schema(description = "챌린지 아이디", example = "301")
        private Long challengeId;

        @Schema(description = "챌린지 제목", example = "자잘자잘")
        private String title;

        @Schema(description = "챌린지 간단 설명", example = "하루 5분씩 무엇이든 꼭 해야...")
        private String description;

        @JsonProperty("image")
        @Schema(description = "챌린지 대표 이미지 URL", example = "http://example.com/challenge_301.jpg")
        private String image;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "종료한 챌린지 정보 DTO")
    public static class CompletedChallengeDto {

        @Schema(description = "챌린지 아이디", example = "301")
        private Long challengeId;

        @Schema(description = "챌린지 제목", example = "자잘자잘")
        private String title;

        @Schema(description = "챌린지 간단 설명", example = "하루 5분씩 무엇이든 꼭 해야...")
        private String description;

        @JsonProperty("image")
        @Schema(description = "챌린지 대표 이미지 URL", example = "http://example.com/challenge_301.jpg")
        private String image;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "스크랩한 인증글 목록 항목 DTO")
    public static class ScrappedVerificationDto {

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

        @Schema(description = "외부 링크 포함 여부 (true: 링크 있음, false: 없음)", example = "true")
        private Boolean hasLink;

        @Schema(description = "질문글 여부 (Q 마크 표시)", example = "true")
        private Boolean isQuestion;

        @Schema(description = "질문 해결 여부 (채택 완료됨 - 정렬 후순위)", example = "false")
        private Boolean isResolved;

        @Schema(description = "작성자 닉네임", example = "해빗")
        private String writerNickname;

        @Schema(description = "작성자 프로필 URL", example = "https://example.com/profile.jpg")
        private String writerProfileUrl;

        @Schema(description = "작성자 ID (본인 글 확인용)", example = "1")
        private Long writerId;

        @Schema(description = "작성일자", example = "2025.12.05")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd")
        private LocalDateTime createdDate;
    }
}
