package com.hrr.backend.domain.follow.dto;

import com.hrr.backend.domain.follow.entity.Follow;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "팔로우 요청 정보 DTO")
public class FollowRequestDto {

    @Schema(description = "팔로우 ID", example = "1")
    private Long followId;

    @Schema(description = "요청자 사용자 ID", example = "123")
    private Long requesterId;

    @Schema(description = "요청자 닉네임", example = "테스트유저")
    private String requesterNickname;

    @Schema(description = "요청자 프로필 이미지", example = "https://example.com/profile.jpg")
    private String requesterProfileImage;

    @Schema(description = "요청 시간", example = "2024-01-15T10:30:00")
    private LocalDateTime requestedAt;

    public static FollowRequestDto from(Follow follow) {
        return FollowRequestDto.builder()
                .followId(follow.getId())
                .requesterId(follow.getFollower().getId())
                .requesterNickname(follow.getFollower().getNickname())
                .requesterProfileImage(follow.getFollower().getProfileImage())
                .requestedAt(follow.getCreatedAt())
                .build();
    }
}