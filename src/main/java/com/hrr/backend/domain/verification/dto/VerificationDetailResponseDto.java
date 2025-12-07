package com.hrr.backend.domain.verification.dto;

import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 인증 상세 조회 응답 DTO
 * 추후 상세 조회 API 구현 시 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인증 상세 조회 응답 DTO")
public class VerificationDetailResponseDto {

    private Long verificationId;

    private Long roundId;

    private Integer roundNumber;

    private Long challengeId;

    private String challengeName;

    private VerificationPostType type;

    private String title;

    private String content;

    private String textUrl;

    private String photoUrl;

    private Boolean isQuestion;

    private VerificationStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private UserInfo author;

    private RoundInfo roundInfo;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;

        private String nickname;

        private String profileImageUrl;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundInfo {
        private Long roundId;

        private Integer roundNumber;

        private String startDate;

        private String endDate;

        private Integer verificationCount;

        private Integer warnCount;
    }
}