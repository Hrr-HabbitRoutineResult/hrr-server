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

    @Schema(description = "닉네임", example = "흐르르")
    @Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하여야 합니다")
    private String nickname;

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/newprofile.jpg")
    private String profileImage;

    @Schema(description = "프로필 공개 여부", example = "false")
    private Boolean isPublic;
}