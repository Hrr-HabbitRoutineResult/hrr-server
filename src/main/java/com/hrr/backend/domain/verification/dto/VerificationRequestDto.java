package com.hrr.backend.domain.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "글 인증 생성 요청 DTO")
public class VerificationRequestDto {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private String textUrl;

    // textImage1/2/3 (null 허용, 최대 3개 필드)
    private String textImage1;
    private String textImage2;
    private String textImage3;

    private Boolean isQuestion;
}