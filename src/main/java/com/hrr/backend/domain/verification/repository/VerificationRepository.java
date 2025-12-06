package com.hrr.backend.domain.verification.repository;

import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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

    @Query("SELECT v FROM Verification v " +
            "JOIN v.roundRecord r " +
            "JOIN r.userChallenge uc " +
            "WHERE uc.user.id = :userId " +
            "AND uc.challenge.id = :challengeId " +
            "AND v.createdAt BETWEEN :start AND :end " +
            "AND v.status = :status")
    List<Verification> findWeeklyVerifications(
            @Param("userId") Long userId,
            @Param("challengeId") Long challengeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") VerificationStatus status
    );


    /**
     * 특정 라운드의 특정 사용자 인증 목록 조회
     */
    @Query("SELECT v FROM Verification v " +
            "JOIN FETCH v.userChallenge uc " +
            "JOIN FETCH uc.user " +
            "WHERE v.roundId = :roundId " +
            "AND v.userChallenge.id = :userChallengeId " +
            "ORDER BY v.createdAt DESC")
    List<Verification> findByRoundIdAndUserChallengeId(
            @Param("roundId") Long roundId,
            @Param("userChallengeId") Long userChallengeId
    );

    /**
     * 특정 라운드의 모든 인증 목록 조회
     */
    @Query("SELECT v FROM Verification v " +
            "JOIN FETCH v.userChallenge uc " +
            "JOIN FETCH uc.user " +
            "WHERE v.roundId = :roundId " +
            "ORDER BY v.createdAt DESC")
    Page<Verification> findByRoundId(@Param("roundId") Long roundId, Pageable pageable);

    /**
     * 사용자의 모든 인증 목록 조회 (챌린지별)
     */
    @Query("SELECT v FROM Verification v " +
            "JOIN FETCH v.userChallenge uc " +
            "WHERE uc.user.id = :userId " +
            "AND uc.challenge.id = :challengeId " +
            "ORDER BY v.createdAt DESC")
    Page<Verification> findByUserIdAndChallengeId(
            @Param("userId") Long userId,
            @Param("challengeId") Long challengeId,
            Pageable pageable
    );

    /**
     * 특정 기간 동안의 인증 개수 조회
     */
    @Query("SELECT COUNT(v) FROM Verification v " +
            "WHERE v.userChallenge.id = :userChallengeId " +
            "AND v.roundId = :roundId " +
            "AND v.status = :status " +
            "AND v.createdAt BETWEEN :startDate AND :endDate")
    Long countByUserChallengeAndRoundAndDateRange(
            @Param("userChallengeId") Long userChallengeId,
            @Param("roundId") Long roundId,
            @Param("status") VerificationStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}