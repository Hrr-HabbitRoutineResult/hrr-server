package com.hrr.backend.domain.verification.dto;

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
    private String textImage1;
    private String textImage2;
    private String textImage3;
}
