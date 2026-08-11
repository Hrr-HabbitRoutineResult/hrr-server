package com.hrr.backend.domain.verification.repository;

import com.hrr.backend.domain.verification.entity.VerificationScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VerificationScrapRepository extends JpaRepository<VerificationScrap, Long> {

    @Modifying
    @Query(
            value = """
                    INSERT IGNORE INTO verification_scrap (user_id, verification_id, created_at, updated_at)
                    VALUES (:userId, :verificationId, NOW(6), NOW(6))
                    """,
            nativeQuery = true
    )
    void insertIgnore(@Param("userId") Long userId, @Param("verificationId") Long verificationId);

    void deleteByUserIdAndVerificationId(Long userId, Long verificationId);
}
