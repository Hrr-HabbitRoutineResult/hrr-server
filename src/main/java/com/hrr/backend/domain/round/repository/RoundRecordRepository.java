package com.hrr.backend.domain.round.repository;

import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.global.common.enums.ChallengeDays;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoundRecordRepository extends JpaRepository<RoundRecord, Long> {

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

    /** 사용자와 라운드 ID로 RoundRecord 조회 (응답 여부 확인용) */
    @Query("SELECT rr FROM RoundRecord rr " +
            "JOIN rr.userChallenge uc " +
            "WHERE uc.user = :user AND rr.round.id = :roundId")
    Optional<RoundRecord> findByUserAndRoundId(
            @Param("user") User user,
            @Param("roundId") Long roundId
    );

	// 챌린지의 특정 라운드의 현재 참가 인원을 조회하는데, 유저의 특정 상태로 필터링 - 인증 통계 조회를 위함
	// 다음 라운드 연장을 하지 않더라도(ChallengeJoinStatus=DROPPED) 인증 통계의 총인원에는 포함되어야 하기에 해당 enum은 쿼리에 포함시키지 않음
	@Query("SELECT COUNT(rr) FROM RoundRecord rr " +
		"WHERE rr.round.id = :roundId " +
		"AND rr.userChallenge.user.userStatus = :status")
	int countParticipantsByRoundAndUserStatus(@Param("roundId") Long roundId, @Param("status") UserStatus status);

	// 라운드가 진행 중인 챌린지에 참여 중인 유저가 어제 인증요일이었는데 미인증 한 사실이 있는지 조회
	@Query("SELECT rr FROM RoundRecord rr " +
		"JOIN rr.userChallenge uc " +
		"JOIN uc.challenge c " +
		"JOIN c.challengeDays cd " +
		"WHERE c.status = com.hrr.backend.global.common.enums.ChallengeStatus.ONGOING " + // 진행 중인 챌린지
		"AND uc.status = com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus.JOINED " + // 참여 중인 유저
		"AND rr.round = c.currentRound " +	// 현재 라운드만 확인
		"AND cd.dayOfWeek = :yesterdayChallengeDay " + // 어제 요일이 인증 요일인지 확인
		"AND NOT EXISTS ( " +
		"    SELECT v FROM Verification v " +
		"    WHERE v.roundRecord = rr " +
		"    AND CAST(v.createdAt AS LocalDate) = :yesterdayDate " + // 어제 날짜의 인증 기록이 없는지 확인 - CAST를 통해 LocalDate로의 캐스팅 효과
		")")
	List<RoundRecord> findAbsentees(
		@Param("yesterdayChallengeDay") ChallengeDays yesterdayChallengeDay,
		@Param("yesterdayDate") LocalDate yesterdayDate
	);
}
