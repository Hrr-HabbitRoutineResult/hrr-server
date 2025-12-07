package com.hrr.backend.domain.verification.dto;

import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class VerificationListResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private Long verificationId;
        private Long challengeId;
        private Long userChallengeId;

        private String nickname;
        private String profileImage;

        private VerificationPostType type;
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

    /** 인증글 목록 + 페이지 정보 */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        private com.hrr.backend.global.response.PageResponseDto<ListItem> posts;
        private boolean empty;
    }
}
