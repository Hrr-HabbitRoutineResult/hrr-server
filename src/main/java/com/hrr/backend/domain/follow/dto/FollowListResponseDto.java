package com.hrr.backend.domain.follow.dto;

import com.hrr.backend.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "팔로워/팔로잉 목록 응답 DTO")
public class FollowListResponseDto {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "닉네임", example = "john")
    private String nickname;

    @Schema(description = "레벨", example = "gold")
    private String level;

    @Schema(description = "프로필 이미지 URL", example = "https://image1.com")
    private String profilePhoto;

    @Schema(description = "현재 로그인한 사용자가 해당 사용자를 팔로우하는지 여부", example = "true")
    private Boolean isFollowing;

    public static FollowListResponseDto of(User user, Boolean isFollowing) {
        return FollowListResponseDto.builder()
                .id(user.getId())
                .nickname(user.getDisplayNickname())
                .level(user.getUserLevel().name().toLowerCase())
                .profilePhoto(user.getProfileImage())
                .isFollowing(isFollowing)
                .build();
    }
}
