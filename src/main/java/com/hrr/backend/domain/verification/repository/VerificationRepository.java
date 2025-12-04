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
    /**사용자 본인 인증글 목록 조회*/
    Page<Verification> findByUserChallenge_User_Id(Long userId, Pageable pageable);

    /**challengeId + roundId로 상세 필터링 조회 -> 내꺼만 조회*/
    Page<Verification> findByUserChallenge_User_IdAndUserChallenge_Challenge_IdAndRoundId(
            Long userId,
            Long challengeId,
            Long roundId,
            Pageable pageable
    );
    /** 챌린지 + 라운드 전체 인증글 조회 */
    Page<Verification> findByUserChallenge_Challenge_Id(Long challengeId, Pageable pageable);

    Page<Verification> findByUserChallenge_Challenge_IdAndRoundId(
            Long challengeId,
            Long roundId,
            Pageable pageable
    );
}