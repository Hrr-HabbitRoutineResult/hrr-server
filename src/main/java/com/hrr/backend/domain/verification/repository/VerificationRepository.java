package com.hrr.backend.domain.verification.repository;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface VerificationRepository extends JpaRepository<Verification, Long>, VerificationRepositoryCustom {

	// 비관적 락 걸면서 id로 조회
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT v FROM Verification v WHERE v.id = :id")
	Optional<Verification> findByIdWithPessimisticLock(@Param("id") Long id);

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

    // 가장 최근 인증 날짜 조회 (COMPLETED 상태만)
    @Query("SELECT MAX(v.createdAt) FROM Verification v " +
            "JOIN v.roundRecord r " +
            "WHERE r.round.id = :roundId " +
            "AND v.status = :status")
    LocalDateTime findLatestVerificationTime(
            @Param("roundId") Long roundId,
            @Param("status") VerificationStatus status
    );

    // 특정 날짜(범위)의 인증 인원 수 (중복 제거, COMPLETED 상태만)
    @Query("""
    SELECT COUNT(DISTINCT r.userChallenge.id) FROM Verification v 
    JOIN v.roundRecord r 
    JOIN r.userChallenge uc 
    JOIN uc.user u 
    WHERE r.round.id = :roundId 
    AND v.status = :status 
    AND u.userStatus = :userStatus 
    AND v.createdAt BETWEEN :start AND :end
""")
    Long countDistinctCertifiers(
            @Param("roundId") Long roundId,
            @Param("status") VerificationStatus status,
            @Param("userStatus") UserStatus userStatus,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
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
			"AND v.status != 'BLOCKED' "	+
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
			"AND v.status != 'BLOCKED' " +
            "ORDER BY v.createdAt DESC")
    Page<Verification> findByRoundId(@Param("roundId") Long roundId, Pageable pageable);

    /**
     * 사용자의 모든 인증 목록 조회 (챌린지별)
     */
    @Query("SELECT v FROM Verification v " +
            "JOIN FETCH v.userChallenge uc " +
            "WHERE uc.user.id = :userId " +
			"AND v.status != 'BLOCKED' " +
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

    // 내 인증 모아보기
    @Query("SELECT v FROM Verification v " +
            "JOIN FETCH v.roundRecord r " +
            "WHERE r.userChallenge.id = :userChallengeId " +
            "AND v.status = :status " +
            "ORDER BY " +
            "  CASE WHEN (v.isQuestion = true AND v.isResolved = false) THEN 0 ELSE 1 END ASC, " +
            "  v.createdAt DESC")
    Page<Verification> findMyVerifications(
            @Param("userChallengeId") Long userChallengeId,
            @Param("status") VerificationStatus status,
            Pageable pageable
    );

    /**
     * 사용자의 전체 챌린지 인증 기록 조회 (페이징)
     */
    @Query("SELECT v FROM Verification v " +
            "JOIN FETCH v.roundRecord rr " +
            "JOIN FETCH rr.userChallenge uc " +
            "JOIN FETCH uc.challenge c " +
            "WHERE uc.user = :user " +
            "AND v.status = :status " +
            "ORDER BY v.createdAt DESC")
    Slice<Verification> findVerificationHistoryByUser(
            @Param("user") User user,
            @Param("status") VerificationStatus status,
            Pageable pageable
    );

}