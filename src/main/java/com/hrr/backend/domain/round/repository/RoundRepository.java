package com.hrr.backend.domain.round.repository;

import java.util.Optional;

import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.global.common.enums.ChallengeDays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoundRepository extends JpaRepository<Round, Long> {
    /**
     * 챌린지의 모든 라운드 조회
     */
    @Query("SELECT r FROM Round r " +
            "WHERE r.challenge.id = :challengeId " +
            "ORDER BY r.roundNumber ASC")
    List<Round> findAllByChallengeId(@Param("challengeId") Long challengeId);

    /**
     * 챌린지의 특정 라운드 번호로 라운드 조회
     */
    @Query("SELECT r FROM Round r " +
            "WHERE r.challenge.id = :challengeId " +
            "AND r.roundNumber = :roundNumber")
    Optional<Round> findByChallengeIdAndRoundNumber(
            @Param("challengeId") Long challengeId,
            @Param("roundNumber") Integer roundNumber
    );

    /**
     * 현재 진행 중인 라운드 조회
     */
    @Query("SELECT r FROM Round r " +
            "WHERE r.challenge.id = :challengeId " +
            "AND :now BETWEEN r.startDate AND r.endDate")
    Optional<Round> findCurrentRoundByChallengeId(
            @Param("challengeId") Long challengeId,
            @Param("now") LocalDate now
    );

    /**
     * 특정 날짜에 종료되는 라운드 목록 조회
     * - Round.challenge는 LAZY일 수 있으므로 JOIN FETCH로 미리 로딩
     * - challenge.currentRound도 접근될 가능성이 있어 LEFT JOIN FETCH로 함께 로딩
     */
    @Query("SELECT r FROM Round r " +
            "JOIN FETCH r.challenge c " +
            "LEFT JOIN FETCH c.currentRound " +
            "WHERE r.endDate = :endDate")
    List<Round> findAllByEndDate(@Param("endDate") LocalDate endDate);

    /**
     * 종료 처리 트랜잭션 내부에서 사용할 Round 재조회용 (detached 방지)
     */
    @Query("SELECT r FROM Round r " +
            "JOIN FETCH r.challenge c " +
            "LEFT JOIN FETCH c.currentRound " +
            "WHERE r.id = :roundId")
    Optional<Round> findByIdWithChallengeAndCurrentRound(@Param("roundId") Long roundId);

    // 챌린지 ID로 모든 라운드를 회차 오름차순(1R, 2R, ...)으로 조회
    List<Round> findAllByChallengeIdOrderByRoundNumberAsc(Long challengeId);

    /**
     * 오늘이 인증 요일이고 현재 진행 중인 라운드 목록 조회
     * - 인증 마감 알림 스케줄링에 사용
     */
    @Query("SELECT r FROM Round r " +
            "JOIN FETCH r.challenge c " +
            "JOIN c.challengeDays cd " +
            "WHERE cd.dayOfWeek = :today " +
            "AND r.startDate <= :now " +
            "AND r.endDate >= :now")
    List<Round> findAllByVerificationDay(
            @Param("today") ChallengeDays today,
            @Param("now") LocalDate now
    );
}