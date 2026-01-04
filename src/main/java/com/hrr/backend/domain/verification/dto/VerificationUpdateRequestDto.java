package com.hrr.backend.domain.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VerificationUpdateRequestDto {

    private String title;
    @Size(max = 200, message = "내용은 200자를 초과할 수 없습니다.")
    private String content;
    private String textUrl;
    @Schema(description = "글 인증 첨부 이미지1(S3 Key)", nullable = true)
    private String textImage1;
    @Schema(description = "글 인증 첨부 이미지2(S3 Key)", nullable = true)
    private String textImage2;
    @Schema(description = "글 인증 첨부 이미지3(S3 Key)", nullable = true)
    private String textImage3;
}
