package com.hrr.backend.domain.verification.repository;

import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.VerificationScrap;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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

    boolean existsByUserIdAndVerificationId(Long userId, Long verificationId);

    @Query("""
            SELECT v FROM VerificationScrap vs
            JOIN vs.verification v
            JOIN FETCH v.roundRecord rr
            JOIN FETCH rr.userChallenge uc
            JOIN FETCH uc.user author
            JOIN FETCH uc.challenge c
            WHERE vs.user.id = :currentUserId
            AND v.status = :status
            AND (:type IS NULL OR v.type = :type)
            AND author.userStatus = com.hrr.backend.domain.user.entity.enums.UserStatus.ACTIVE
            AND NOT EXISTS (
                SELECT 1 FROM UserBlock b
                WHERE b.blocker.id = :currentUserId
                AND b.blocked.id = author.id
            )
            AND NOT EXISTS (
                SELECT 1 FROM UserBlock b
                WHERE b.blocker.id = author.id
                AND b.blocked.id = :currentUserId
            )
            AND (
                c.isPublic = true
                OR author.id = :currentUserId
                OR EXISTS (
                    SELECT 1 FROM UserChallenge viewerUc
                    WHERE viewerUc.user.id = :currentUserId
                    AND viewerUc.challenge = c
                    AND viewerUc.status = com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus.JOINED
                )
            )
            ORDER BY vs.createdAt DESC
            """)
    Slice<Verification> findVisibleScrappedVerifications(
            @Param("currentUserId") Long currentUserId,
            @Param("type") VerificationPostType type,
            @Param("status") VerificationStatus status,
            Pageable pageable
    );
}
