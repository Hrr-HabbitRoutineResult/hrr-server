package com.hrr.backend.domain.verification.dto;

import com.hrr.backend.global.response.PageResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class VerificationMyResponseDto {

    /**
     * 인증글 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MyPost {

        private Long verificationId;
        private Long challengeId;
        private Long userChallengeId;

        private String title;
        private String content;
        private String photoUrl;
        private String textUrl;

        private Boolean isQuestion;
        private Long roundId;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private String challengeTitle;
    }

    /**
     * 인증글 목록 DTO (페이지네이션 래핑)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MyPostList {
        private PageResponseDto<MyPost> posts;
    }
}
