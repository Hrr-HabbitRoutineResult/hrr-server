package com.hrr.backend.domain.round.repository;

import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}