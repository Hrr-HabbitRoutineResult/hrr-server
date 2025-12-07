package com.hrr.backend.domain.follow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "팔로우/팔로우 취소 응답 DTO")
public class FollowResponseDto {

    @Schema(description = "응답 메시지", example = "User followed successfully")
    private String message;

    @Schema(description = "팔로우/팔로우 취소된 사용자 ID", example = "123")
    private Long userId;

    public static FollowResponseDto of(String message, Long userId) {
        return FollowResponseDto.builder()
                .message(message)
                .userId(userId)
                .build();
    }
}