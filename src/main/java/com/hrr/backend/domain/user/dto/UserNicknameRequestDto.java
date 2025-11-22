package com.hrr.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UserNicknameRequestDto {

    @Schema(description = "닉네임을 설정해주세요(최대 10자)", example = "김흐르")
    private String nickname;
}
