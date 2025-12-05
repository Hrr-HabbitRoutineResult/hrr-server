package com.hrr.backend.domain.verification.repository;

import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface VerificationRepository extends JpaRepository<Verification, Long> {

    // 오늘 완료된 인증이 있는지 확인
    @Query("""
        SELECT COUNT(v) > 0 
        FROM Verification v 
        WHERE v.roundRecord.userChallenge.id = :userChallengeId 
        AND v.status = :status 
        AND v.createdAt >= :startOfDay 
        AND v.createdAt <= :endOfDay
    """)
    boolean existsTodayVerification(
            @Param("userChallengeId") Long userChallengeId,
            @Param("status") VerificationStatus status,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );

    // 피드 조회
    // 조건: 라운드ID, 타입(사진/글), 상태(COMPLETED)
    // 정렬: 미해결 질문(0) -> 그 외(1), 이후 최신순
    @Query("SELECT v FROM Verification v " +
            "JOIN FETCH v.roundRecord r " +
            "JOIN FETCH r.userChallenge uc " +
            "JOIN FETCH uc.user u " +
            "WHERE r.round.id = :roundId " +
            "AND v.type = :type " +
            "AND v.status = :status " +
            "ORDER BY " +
            "  CASE WHEN (v.isQuestion = true AND v.isResolved = false) THEN 0 ELSE 1 END ASC, " +
            "  v.createdAt DESC")
    Page<Verification> findVerificationFeed(
            @Param("roundId") Long roundId,
            @Param("type") VerificationPostType type,
            @Param("status") VerificationStatus status,
            Pageable pageable
    );

}