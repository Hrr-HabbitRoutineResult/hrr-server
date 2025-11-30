package com.hrr.backend.domain.verification.dto;

import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 인증글 상세 조회용 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationDetailResponseDto {

    private Long verificationId;

    private Long challengeId;
    private Long userChallengeId;

    /** 작성자 정보 */
    private Long userId;
    private String nickname;
    private String profileImage;

    /** 본문 정보 */
    private VerificationPostType type;
    private String title;
    private String content;
    private String photoUrl;
    private String textUrl;
    private Boolean isQuestion;
    private VerificationStatus status;

    /** 챌린지 제목 */
    private String challengeTitle;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 라운드 ID */
    private Long roundId;

    /** 좋아요 / 스크랩 / 채택 여부 등은 나중에 붙일 예정 */
}
