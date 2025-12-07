package com.hrr.backend.domain.user.dto;

import com.hrr.backend.domain.user.entity.User;
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
@Schema(description = "사용자 정보 수정 응답")
public class UpdateUserInfoResponseDto {

    @Schema(description = "닉네임", example = "흐르르")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/newprofile.jpg")
    private String profileImage;

    @Schema(description = "프로필 공개 여부", example = "false")
    private Boolean isPublic;

    @Schema(description = "수정 일시", example = "2025-10-10T12:15:00")
    private LocalDateTime updatedAt;

    public static UpdateUserInfoResponseDto from(User user) {
        return UpdateUserInfoResponseDto.builder()
                .nickname(user.getNickname())
                .profileImage(user.getProfileImage())
                .isPublic(user.getIsPublic())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}