package com.hrr.backend.domain.verification.dto;

import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
    private Boolean isResolved;
    private VerificationStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 권한/상태 플래그
    private Boolean isMine;
    private Boolean canEdit;
    private Boolean canDelete;
    private Boolean canSelectComment;

    //프론트가 받기 쉽게
    private boolean canWriteComment;
    private Long adoptedCommentId;
    private boolean showResolvedBadge;
    private int commentCount;

    // 작성자 정보
    private UserInfo user;

    // 라운드 정보
    private RoundInfo roundInfo;

    // 댓글 목록 + 페이징 정보
    private CommentListResponseDto comments;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String nickname;
        private String profileImageUrl;
        private UserChallengeRole role;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RoundInfo {
        private String startDate;
        private String endDate;
        private Integer verificationCount;
        private Integer warnCount;
    }
}
