package com.hrr.backend.domain.verification.repository;

import com.hrr.backend.domain.verification.entity.VerificationScrap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationScrapRepository extends JpaRepository<VerificationScrap, Long> {

    boolean existsByUserIdAndVerificationId(Long userId, Long verificationId);

    void deleteByUserIdAndVerificationId(Long userId, Long verificationId);
}
