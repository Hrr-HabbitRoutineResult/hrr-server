package com.hrr.backend.domain.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class VerificationUpdateRequestDto {

    private String title;
    @Size(max = 200, message = "내용은 200자를 초과할 수 없습니다.")
    private String content;
    private String textUrl;
    @Schema(description = "글 인증 첨부 이미지 리스트(S3 Key), 최대 3개", nullable = true)
    @Size(max = 3, message = "이미지는 최대 3개까지 등록 가능합니다.")
    private List<String> textImages;
    private Boolean isQuestion;
}
