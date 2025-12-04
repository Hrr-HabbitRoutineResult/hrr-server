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
@Schema(description = "팔로우 액션 응답 DTO")
public class FollowActionResponseDto {

    @Schema(description = "응답 메시지", example = "Follow request approved successfully")
    private String message;

    @Schema(description = "팔로우 ID", example = "1")
    private Long followId;

    public static FollowActionResponseDto of(String message, Long followId) {
        return FollowActionResponseDto.builder()
                .message(message)
                .followId(followId)
                .build();
    }
}