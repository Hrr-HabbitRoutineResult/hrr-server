package com.hrr.backend.domain.verification.service;

import com.hrr.backend.domain.verification.dto.VerificationRequestDto;
import com.hrr.backend.domain.verification.dto.VerificationResponseDto;

public interface VerificationService {

    VerificationResponseDto createTextVerification(
            Long challengeId,
            Long userId,
            VerificationRequestDto request
    );

    VerificationResponseDto createPhotoVerification(
            Long challengeId,
            Long userId,
            String content,
            String s3Key,
            String title,
            Boolean isQuestion
    );
}
