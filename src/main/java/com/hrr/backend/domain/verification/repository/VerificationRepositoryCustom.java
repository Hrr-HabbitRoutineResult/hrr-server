package com.hrr.backend.domain.verification.repository;

import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VerificationRepositoryCustom {
    Page<Verification> findVerificationFeed(
            Long roundId,
            VerificationPostType type,
            VerificationStatus status,
            Long currentUserId,
            Pageable pageable
    );
}