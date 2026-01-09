package com.hrr.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정보 수정 요청")
public class UpdateUserInfoRequestDto {

    @Schema(description = "닉네임 (1-20자)", example = "흐르르")
    @Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하여야 합니다")
    private String nickname;

    @Schema(description = "닉네임 변경 여부 (true: nickname 처리, false: nickname 무시)", example = "true")
    private Boolean isNicknameChanged;

    @Schema(description = "프로필 이미지 S3 Key (null이면 기본 이미지로 변경)", example = "users/uuid-profile.jpg")
    private String profileImageKey;

    @Schema(description = "프로필 이미지 변경 여부 (true: profileImageKey 처리, false: profileImageKey 무시)", example = "true")
    private Boolean isProfileImageChanged;
}