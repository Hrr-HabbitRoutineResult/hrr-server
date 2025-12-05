package com.hrr.backend.domain.verification.converter;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.domain.verification.entity.Verification;

@Component
public class VerificationConverter {

    public VerificationResponseDto.FeedDto toFeedDto(Verification verification) {

        return VerificationResponseDto.FeedDto.builder()
                .verificationId(verification.getId())
                .type(verification.getType())
                .title(verification.getTitle())
                .content(verification.getContent())
                .imageUrl(verification.getPhotoUrl())
                .linkUrl(verification.getTextUrl())
                .isQuestion(verification.getIsQuestion())
                .isResolved(verification.getIsResolved())
                .createdDate(verification.getCreatedAt())
                .build();
    }

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

}