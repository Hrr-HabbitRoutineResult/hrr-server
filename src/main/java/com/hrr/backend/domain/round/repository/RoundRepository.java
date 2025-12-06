package com.hrr.backend.domain.round.repository;

import java.util.Optional;

import com.hrr.backend.domain.round.entity.Round;
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
     */
    @Query("SELECT r FROM Round r " +
            "WHERE r.endDate = :endDate")
    List<Round> findAllByEndDate(@Param("endDate") LocalDate endDate);
}