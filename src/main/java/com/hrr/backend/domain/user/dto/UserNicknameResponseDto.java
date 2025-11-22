package com.hrr.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNicknameResponseDto {

    @Schema(description = "설정된 닉네임", example = "김흐르")
    private String nickname;

    @Schema(description = "닉네임 관련 메시지", example = "사용 가능한 닉네임이에요.")
    private String message;

    @Schema(description = "다음 화면/플로우 정보", example = "MAIN")
    private String nextStep;
}
