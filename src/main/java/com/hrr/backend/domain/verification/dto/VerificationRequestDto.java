package com.hrr.backend.domain.verification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Getter
@NoArgsConstructor
public class VerificationRequestDto {

    /** 글 인증 제목 */
    @NotBlank(message = "인증 제목은 필수입니다.")
    private String title;

    /** 글 인증 내용 */
    @NotBlank(message = "인증 내용은 필수입니다.")
    @Size(max = 200, message = "내용은 200자를 초과할 수 없습니다.")
    private String content;

    /** 질문 등록 여부 */
    private Boolean isQuestion;

    /** 첨부 URL (선택) */
    private String textUrl;

    @Override
    public String toString() {
        return "VerificationRequestDto{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", isQuestion=" + isQuestion +
                ", textUrl='" + textUrl + '\'' +
                '}';
    }
}
