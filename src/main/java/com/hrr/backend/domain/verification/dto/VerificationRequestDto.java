package com.hrr.backend.domain.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @Schema(description = "글 인증 첨부 이미지 리스트(S3 Key), 최대 3개", nullable = true)
    @Size(max = 3, message = "이미지는 최대 3개까지 등록 가능합니다.")
    private List<String> textImages;

    private Boolean isQuestion;
}