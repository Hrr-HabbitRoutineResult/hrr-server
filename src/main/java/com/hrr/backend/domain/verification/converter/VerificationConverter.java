package com.hrr.backend.domain.verification.converter;

import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.domain.verification.entity.Verification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class VerificationConverter {

    public VerificationResponseDto.FeedDto toFeedDto(Verification verification) {
        boolean hasLink = verification.getTextUrl() != null && !verification.getTextUrl().isBlank();

        return VerificationResponseDto.FeedDto.builder()
                .verificationId(verification.getId())
                .type(verification.getType())
                .title(verification.getTitle())
                .content(verification.getContent())
                .imageUrl(verification.getPhotoUrl())
                .hasLink(hasLink)
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