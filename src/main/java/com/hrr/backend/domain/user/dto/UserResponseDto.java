package com.hrr.backend.domain.user.dto;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponseDto {
    private Long userId;
    private String nickname;
    private String profilePhoto;
    private String level; // (User의 Level Enum을 String으로 변환)
    private Long followerCount;
    private Long followingCount;
    private Boolean isFollowing;
}
