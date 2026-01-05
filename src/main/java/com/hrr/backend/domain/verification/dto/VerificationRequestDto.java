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

    @Schema(description = "글 인증 첨부 이미지1(S3 Key)", nullable = true)
    private String textImage1;
    @Schema(description = "글 인증 첨부 이미지2(S3 Key)", nullable = true)
    private String textImage2;
    @Schema(description = "글 인증 첨부 이미지3(S3 Key)", nullable = true)
    private String textImage3;

    private Boolean isQuestion;
}