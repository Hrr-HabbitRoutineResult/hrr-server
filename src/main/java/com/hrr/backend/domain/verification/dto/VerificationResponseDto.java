package com.hrr.backend.domain.verification.dto;

import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponseDto {

    private Long verificationId;
    private Long roundId;
    private Long challengeId;
    private Long userChallengeId;

    /** TEXT / CAMERA */
    private VerificationPostType type;

    private String title;
    private String content;
    private String photoUrl;
    private String textUrl;
    private Boolean isQuestion;

    /** TEMPORARY / COMPLETED */
    private VerificationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long userId;
    private String userNickname;

    /** 채택 완료 여부 */
    private Boolean isAdopted;
    /**현재 라운드 인증 횟수*/
    private Integer verificationCount;
}
