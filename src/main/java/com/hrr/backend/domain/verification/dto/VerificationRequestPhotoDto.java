package com.hrr.backend.domain.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "사진 인증 생성 요청 DTO(JSON)")
public class VerificationRequestPhotoDto {

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "내용")
    private String content;

    @Schema(description = "사진 S3 key (presigned URL 방식 업로드 후 저장)")
    @NotBlank(message = "s3Key는 필수입니다.")
    private String s3Key;

    private Boolean isQuestion = false;
}
