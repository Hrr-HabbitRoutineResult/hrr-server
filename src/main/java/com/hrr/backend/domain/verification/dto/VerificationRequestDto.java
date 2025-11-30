package com.hrr.backend.domain.verification.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerificationRequestDto {

    /** 글 인증 제목 (선택) */
    private String title;

    /** 글 인증 내용 (필수) */
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
