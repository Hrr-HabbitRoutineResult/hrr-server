package com.hrr.backend.domain.verification.converter;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

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
                .writerNickname(writer.getNickname())
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
    public VerificationResponseDto toResponseDto(Verification verification) {
        return VerificationResponseDto.builder()
                .verificationId(verification.getId())
                .roundId(verification.getRoundRecord().getRound().getId()) // RoundRecord 통해 접근
                .challengeId(verification.getRoundRecord().getUserChallenge().getChallenge().getId())
                .type(verification.getType())
                .title(verification.getTitle())
                .content(verification.getContent())
                .textUrl(verification.getTextUrl())
                .photoUrl(s3UrlUtil.toFullUrl(verification.getPhotoUrl())) // S3 Full URL 변환
                .isQuestion(verification.getIsQuestion())
                .status(verification.getStatus())
                .createdAt(verification.getCreatedAt())
                .userId(verification.getRoundRecord().getUserChallenge().getUser().getId())
                .userNickname(verification.getRoundRecord().getUserChallenge().getUser().getNickname())
                .verificationCount(verification.getRoundRecord().getVerificationCount())
                .build();
    }

}