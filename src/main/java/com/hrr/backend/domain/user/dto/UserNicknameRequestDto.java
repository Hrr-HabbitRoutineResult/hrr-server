package com.hrr.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@NoArgsConstructor
public class UserNicknameRequestDto {

    @NotBlank(message = "닉네임은 비워둘 수 없습니다.")
    @Size(max = 10, message = "닉네임은 최대 10자까지 입력 가능합니다.")
    @Schema(description = "닉네임을 설정해주세요(최대 10자)", example = "김흐르")
    private String nickname;
}
