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

    /** 채택 완료 여부 */
    private Boolean isAdopted;

    /** 지금은 null 가능 */
    private Long roundId;
}
