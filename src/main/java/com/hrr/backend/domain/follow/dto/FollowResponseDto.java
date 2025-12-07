package com.hrr.backend.domain.follow.dto;

import com.hrr.backend.domain.follow.entity.enums.FollowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "팔로우 관련 응답 DTO")
public class FollowResponseDto {

    @Schema(description = "응답 메시지", example = "User followed successfully")
    private String message;

    @Schema(description = "관련 ID (userId 또는 followId)", example = "123")
    private Long id;

    @Schema(description = "팔로우 상태", example = "APPROVED")
    private FollowStatus status;

    public static FollowResponseDto of(String message, Long id) {
        return FollowResponseDto.builder()
                .message(message)
                .id(id)
                .status(status)
                .build();
    }
}