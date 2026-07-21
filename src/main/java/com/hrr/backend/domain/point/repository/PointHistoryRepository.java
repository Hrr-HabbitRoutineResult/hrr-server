package com.hrr.backend.domain.point.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.point.entity.PointHistory;
import com.hrr.backend.domain.point.entity.enums.PointType;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.user.entity.User;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    // 챌린지 단위 중복 지급 방지 체크용 (FIRST_VERIFICATION, CHALLENGE_MASTER)
    boolean existsByUserAndPointTypeAndChallenge(User user, PointType pointType, Challenge challenge);

    // 라운드 단위 중복 지급 방지 체크용 (FLAWLESS_ROUND, WEEKx_PERFECT)
    boolean existsByUserAndPointTypeAndRound(User user, PointType pointType, Round round);

    /** 내 포인트 적립 내역 조회 */
    @Query("SELECT p FROM PointHistory p " +
            "LEFT JOIN FETCH p.challenge " +
            "LEFT JOIN FETCH p.randomMission " +
            "WHERE p.user.id = :userId " +
            "AND p.createdAt >= :from " +
            "ORDER BY p.createdAt DESC, p.id DESC")
    Slice<PointHistory> findMyPointHistory(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            Pageable pageable
    );
}