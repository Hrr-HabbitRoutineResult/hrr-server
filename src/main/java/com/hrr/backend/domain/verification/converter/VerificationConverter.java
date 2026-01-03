package com.hrr.backend.domain.verification.converter;

import java.time.LocalDateTime;

import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.domain.round.entity.RoundRecord;
import org.springframework.stereotype.Component;
import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationDetailResponseDto;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.s3.S3UrlUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VerificationConverter {

    private final S3UrlUtil s3UrlUtil;


    /** 인증 피드 목록 항목 변환 */
    public VerificationResponseDto.FeedDto toFeedDto(Verification verification) {
        User writer = verification.getRoundRecord().getUserChallenge().getUser();

        // 텍스트 링크 존재 여부
        boolean hasLink = verification.getTextUrl() != null && !verification.getTextUrl().isBlank();

        // 이미지 URL S3 Full Path 변환 (필요 시)
        String fullImageUrl = verification.getPhotoUrl() != null ? 
                s3UrlUtil.toFullUrl(verification.getPhotoUrl()) : null;

        return VerificationResponseDto.FeedDto.builder()
                .verificationId(verification.getId())
                .type(verification.getType())
                .title(verification.getTitle())
                .content(verification.getContent())
                .imageUrl(fullImageUrl)
                .hasLink(hasLink)
                .isQuestion(verification.getIsQuestion())
                .isResolved(verification.getIsResolved())
                .writerNickname(writer.getDisplayNickname())
                .writerProfileUrl(writer.getProfileImage())
                .writerId(writer.getId())
                .createdDate(verification.getCreatedAt())
                .build();
    }

    /** 인증 통계 정보 변환 */
    public VerificationResponseDto.StatDto toStatDto(
            Integer certifiedCount,
            Integer totalParticipantCount,
            LocalDateTime baseDate
    ) {
        return VerificationResponseDto.StatDto.builder()
                .certifiedCount(certifiedCount)
                .totalParticipantCount(totalParticipantCount)
                .baseDate(baseDate)
                .build();
    }

    /**
     * 인증글 생성 직후 응답 DTO 변환 (단건 상세)
     */
    public VerificationResponseDto.CreateResponseDto toResponseDto(Verification verification) {
        return VerificationResponseDto.CreateResponseDto.builder()
                .verificationId(verification.getId())
                .roundId(verification.getRoundRecord().getRound().getId())
                .challengeId(verification.getRoundRecord().getUserChallenge().getChallenge().getId())
                .type(verification.getType())
                .title(verification.getTitle())
                .content(verification.getContent())
                .textUrl(verification.getTextUrl())
                .photoUrl(s3UrlUtil.toFullUrl(verification.getPhotoUrl()))
                .isQuestion(verification.getIsQuestion())
                .status(verification.getStatus())
                .createdAt(verification.getCreatedAt())
                .userId(verification.getRoundRecord().getUserChallenge().getUser().getId())
                .userNickname(verification.getRoundRecord().getUserChallenge().getUser().getDisplayNickname())
                .verificationCount(verification.getRoundRecord().getVerificationCount())
                .build();
    }

    /**
     * 챌린지 인증 현황 마이 DTO 변환
     */
    public VerificationResponseDto.MyProfileDto toMyProfileDto(
            UserChallenge userChallenge,
            Long totalVerificationCount,
            Long currentRoundSequence,
            SliceResponseDto<VerificationResponseDto.FeedDto> verifications
    ) {
        return VerificationResponseDto.MyProfileDto.builder()
                .nickname(userChallenge.getUser().getDisplayNickname())
                .totalVerificationCount(totalVerificationCount)
                .warningCount(userChallenge.getKickWarnings())
                .currentRoundSequence(currentRoundSequence)
                .verifications(verifications)
                .build();
    }
    public VerificationDetailResponseDto toDetailDto(
            Verification verification,
            CommentListResponseDto comments,
            boolean isMine,
            boolean canEdit,
            boolean canDelete,
            boolean canSelectComment,
            Long adoptedCommentId
    ) {
        RoundRecord roundRecord = verification.getRoundRecord();
        Round round = roundRecord.getRound();
        Challenge challenge = round.getChallenge();
        UserChallenge userChallenge = roundRecord.getUserChallenge();
        User user = userChallenge.getUser();

        String photoUrl = s3UrlUtil.toFullUrl(verification.getPhotoUrl());

        VerificationDetailResponseDto.UserInfo userInfo =
                VerificationDetailResponseDto.UserInfo.builder()
                        .userId(user.getId())
                        .nickname(user.getDisplayNickname())
                        .profileImageUrl(user.getProfileImage())
                        .level(userChallenge.getUser().getUserLevel())
                        .build();

        VerificationDetailResponseDto.RoundInfo roundInfo =
                VerificationDetailResponseDto.RoundInfo.builder()
                        .startDate(round.getStartDate() != null ? round.getStartDate().toString() : null)
                        .endDate(round.getEndDate() != null ? round.getEndDate().toString() : null)
                        .verificationCount(roundRecord.getVerificationCount())
                        .warnCount(roundRecord.getWarnCount())
                        .build();

        return VerificationDetailResponseDto.builder()
                .verificationId(verification.getId())
                .roundId(round.getId())
                .roundNumber(round.getRoundNumber())
                .challengeId(challenge.getId())
                .challengeName(challenge.getTitle())
                .type(verification.getType())
                .title(verification.getTitle())
                .content(verification.getContent())
                .textUrl(verification.getTextUrl())
                .photoUrl(photoUrl)
                .isQuestion(verification.getIsQuestion())
                .isResolved(verification.getIsResolved())
                .status(verification.getStatus())
                .createdAt(verification.getCreatedAt())
                .updatedAt(verification.getUpdatedAt())
                .user(userInfo)
                .roundInfo(roundInfo)
                .isMine(isMine)
                .canEdit(canEdit)
                .canDelete(canDelete)
                .canSelectComment(canSelectComment)
                .canWriteComment(true) // 정책 상 언제든 댓글 작성 가능
                .adoptedCommentId(adoptedCommentId)
                .showResolvedBadge(verification.getIsResolved())
                .commentCount(comments != null && comments.getComments() != null
                        ? comments.getComments().size()
                        : 0)
                .comments(comments)
                .build();
    }


    /**
     * Verification 엔티티를 HistoryDto로 변환
     */
    public VerificationResponseDto.HistoryDto toHistoryDto(Verification verification) {
        return VerificationResponseDto.HistoryDto.builder()
                .verificationId(verification.getId())
                .challengeId(verification.getRoundRecord().getUserChallenge().getChallenge().getId())
                .challengeTitle(verification.getRoundRecord().getUserChallenge().getChallenge().getTitle())
                .type(verification.getType().name())
                .title(verification.getTitle())
                .content(verification.getContent())
                .photoUrl(verification.getPhotoUrl() != null ?
                        s3UrlUtil.toFullUrl(verification.getPhotoUrl()) : null)
                .textUrl(verification.getTextUrl())
                .verifiedAt(verification.getCreatedAt())
                .build();
    }
}
