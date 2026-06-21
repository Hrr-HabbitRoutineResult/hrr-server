package com.hrr.backend.domain.round.repository;

import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.global.common.enums.ChallengeDays;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

public interface RoundRecordRepository extends JpaRepository<RoundRecord, Long> {

	// 비관적 락 조회
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT rr FROM RoundRecord rr WHERE rr.id = :id")
	Optional<RoundRecord> findByIdWithPessimisticLock(@Param("id") Long id);

    /**
     * UserChallenge와 Round ID로 RoundRecord 조회
     * 인증 생성 시 사용자의 해당 라운드 기록 조회
     */
    @Query("SELECT rr FROM RoundRecord rr " +
            "JOIN FETCH rr.round r " +
            "WHERE rr.userChallenge = :userChallenge " +
            "AND r.id = :roundId")
    Optional<RoundRecord> findByUserChallengeAndRoundId(
            @Param("userChallenge") UserChallenge userChallenge,
            @Param("roundId") Long roundId
    );

    /**
     * 특정 라운드의 모든 RoundRecord 조회
     */
    @Query("SELECT rr FROM RoundRecord rr " +
            "JOIN FETCH rr.userChallenge uc " +
            "JOIN FETCH uc.user " +
            "WHERE rr.round.id = :roundId " +
            "ORDER BY rr.verificationCount DESC")
    List<RoundRecord> findAllByRoundId(@Param("roundId") Long roundId);

    /**
     * 사용자의 모든 RoundRecord 조회
     */
    @Query("SELECT rr FROM RoundRecord rr " +
            "JOIN FETCH rr.round r " +
            "WHERE rr.userChallenge.id = :userChallengeId " +
            "ORDER BY r.roundNumber DESC")
    List<RoundRecord> findAllByUserChallengeId(@Param("userChallengeId") Long userChallengeId);

    // 내가 참여한 라운드 개수 (몇 라운드째 참여 중인지)
    @Query("SELECT COUNT(r) FROM RoundRecord r WHERE r.userChallenge.id = :userChallengeId")
    Long countByUserChallengeId(@Param("userChallengeId") Long userChallengeId);

    // 내 총 인증 횟수 합계
    @Query("SELECT COALESCE(SUM(r.verificationCount), 0) FROM RoundRecord r WHERE r.userChallenge.id = :userChallengeId")
    Long sumVerificationCountByUserChallengeId(@Param("userChallengeId") Long userChallengeId);

    /**
     * 알림 발송 대상자 및 설정 정보 일괄 조회
     */
    @Query("SELECT rr FROM RoundRecord rr " +
            "JOIN FETCH rr.userChallenge uc " +
            "JOIN FETCH uc.user u " +
            "JOIN FETCH u.notificationSetting " +
            "WHERE rr.round = :round " +
            "AND uc.status = :status") // 수정됨
    List<RoundRecord> findAllByRoundWithUserAndSetting(
            @Param("round") Round round,
            @Param("status") ChallengeJoinStatus status
    );

    /**멱등성 위한 exists 메서드 추가-> 스케줄러 장애 또는 재시작으로 재실행돼도 중복 생성 방지하게*/
    boolean existsByUserChallengeAndRound(UserChallenge userChallenge, Round round);

    /**
	 * 사용자와 라운드 ID 목록으로 RoundRecord 일괄 조회 (응답 여부 확인용)
	 */
    @Query("SELECT rr FROM RoundRecord rr " +
            "JOIN FETCH rr.round r " +
            "JOIN rr.userChallenge uc " +
            "WHERE uc.user = :user AND r.id IN :roundIds")
    List<RoundRecord> findByUserAndRoundIds(
            @Param("user") User user,
            @Param("roundIds") List<Long> roundIds
    );
	
	// 라운드가 진행 중인 챌린지에 참여 중인 유저가 어제 인증요일이었는데 미인증 한 사실이 있는지 조회
	@Query("SELECT rr FROM RoundRecord rr " +
		"JOIN rr.userChallenge uc " +
		"JOIN uc.challenge c " +
		"JOIN c.challengeDays cd " +
		"JOIN rr.round r " +
		"WHERE c.status = com.hrr.backend.global.common.enums.ChallengeStatus.ONGOING " + // 진행 중인 챌린지
		"AND uc.status = com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus.JOINED " + // 참여 중인 유저
		"AND cd.dayOfWeek = :yesterdayChallengeDay " + // 어제 요일이 인증 요일인지 확인
		"AND r.startDate <= :yesterdayDate " +		// 어제 날짜 기준, 해당 라운드의 기간 내에 포함되는지 확인
		"AND r.endDate >= :yesterdayDate " +
		"AND NOT EXISTS ( " +
		"    SELECT v FROM Verification v " +
		"    WHERE v.roundRecord = rr " +
		"    AND CAST(v.createdAt AS LocalDate) = :yesterdayDate " + // 어제 날짜의 인증 기록이 없는지 확인 - CAST를 통해 LocalDate로의 캐스팅 효과
		")")
	List<RoundRecord> findAbsentees(
		@Param("yesterdayChallengeDay") ChallengeDays yesterdayChallengeDay,
		@Param("yesterdayDate") LocalDate yesterdayDate
	);

	@Query("""
    SELECT rr FROM RoundRecord rr 
    JOIN FETCH rr.userChallenge uc 
    JOIN FETCH uc.user u 
    JOIN FETCH u.notificationSetting 
    WHERE rr.round = :round 
    AND uc.status = :joinStatus 
    AND NOT EXISTS (
        SELECT v FROM Verification v 
        WHERE v.roundRecord = rr 
        AND v.status = :verificationStatus 
        AND v.createdAt >= :startOfDay 
        AND v.createdAt < :endOfDay
    )
""")
	List<RoundRecord> findAllByRoundAndNotVerifiedToday(
			@Param("round") Round round,
			@Param("joinStatus") ChallengeJoinStatus joinStatus,
			@Param("verificationStatus") VerificationStatus verificationStatus,
			@Param("startOfDay") LocalDateTime startOfDay,
			@Param("endOfDay") LocalDateTime endOfDay
	);

	/**
	 * 챌린지의 특정 라운드에서 특정 참여 상태(JOINED)인 총 인원 조회
	 * (탈퇴자 포함, 퇴출자 제외)
	 */
	@Query("""
    SELECT COUNT(rr)
    FROM RoundRecord rr
    JOIN rr.userChallenge uc
    WHERE rr.round.id = :roundId
    AND uc.status = :status
""")
	int countByRoundIdAndJoinStatus(
			@Param("roundId") Long roundId,
			@Param("status") ChallengeJoinStatus status
	);
}
