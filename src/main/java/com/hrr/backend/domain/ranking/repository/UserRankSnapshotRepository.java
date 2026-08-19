package com.hrr.backend.domain.ranking.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrr.backend.domain.ranking.entity.UserRankSnapshot;

public interface UserRankSnapshotRepository extends JpaRepository<UserRankSnapshot, Long> {

    // 가장 최근 스냅샷 기준일 조회 (아직 스냅샷이 한 번도 생성되지 않았으면 empty)
    @Query("SELECT MAX(s.snapshotDate) FROM UserRankSnapshot s")
    Optional<LocalDate> findLatestSnapshotDate();

    // 특정 스냅샷일의 등수순 상위 N명 조회
    @Query("SELECT s FROM UserRankSnapshot s " +
            "JOIN FETCH s.user u " +
            "WHERE s.snapshotDate = :snapshotDate " +
            "AND u.userStatus = com.hrr.backend.domain.user.entity.enums.UserStatus.ACTIVE " +
            "ORDER BY s.ranking ASC, CASE WHEN s.achievedAt IS NULL THEN 1 ELSE 0 END ASC, s.achievedAt ASC")
    List<UserRankSnapshot> findTopByRanking(@Param("snapshotDate") LocalDate snapshotDate, Pageable pageable);

    // 내 등수를 활성 유저 기준으로 계산
    @Query("SELECT COUNT(s) FROM UserRankSnapshot s " +
            "JOIN s.user u " +
            "WHERE s.snapshotDate = :snapshotDate " +
            "AND u.userStatus = com.hrr.backend.domain.user.entity.enums.UserStatus.ACTIVE " +
            "AND s.points > :points")
    long countHigherRankers(@Param("snapshotDate") LocalDate snapshotDate, @Param("points") Long points);

    /** 특정 스냅샷일의 활성 유저 총 인원수. */
    @Query("SELECT COUNT(s) FROM UserRankSnapshot s " +
            "JOIN s.user u " +
            "WHERE s.snapshotDate = :snapshotDate " +
            "AND u.userStatus = com.hrr.backend.domain.user.entity.enums.UserStatus.ACTIVE")
    long countActiveBySnapshotDate(@Param("snapshotDate") LocalDate snapshotDate);

    // 특정 유저의 특정 스냅샷일 기록 조회
    Optional<UserRankSnapshot> findByUserIdAndSnapshotDate(Long userId, LocalDate snapshotDate);

    // 특정 기준일 이전의 가장 최근 스냅샷 조회
    @Query("SELECT s FROM UserRankSnapshot s " +
            "WHERE s.user.id = :userId " +
            "AND s.snapshotDate < :beforeDate " +
            "ORDER BY s.snapshotDate DESC")
    List<UserRankSnapshot> findPreviousSnapshots(
            @Param("userId") Long userId,
            @Param("beforeDate") LocalDate beforeDate,
            Pageable pageable
    );

    /** 해당 스냅샷일의 행이 이미 존재하는지 확인. */
    boolean existsBySnapshotDate(LocalDate snapshotDate);

    /** 지정한 스냅샷일 기준으로 전체 ACTIVE 유저의 주간 랭킹 스냅샷을 생성한다. (컷오프 이전 포인트만 집계) */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
		INSERT INTO user_rank_snapshot (user_id, ranking, points, total_user_count, snapshot_date, achieved_at, created_at, updated_at)
		SELECT
		    t.id,
		    RANK() OVER (ORDER BY t.points DESC),
		    t.points,
		    t.total_count,
		    :snapshotDate,
		    t.achieved_at,
		    NOW(),
		    NOW()
		FROM (
		    SELECT
		        u.id                        AS id,
		        COALESCE(SUM(ph.points), 0) AS points,
		        COUNT(*) OVER ()            AS total_count,
		        MAX(ph.created_at)          AS achieved_at
		    FROM user u
		    LEFT JOIN point_history ph
		           ON ph.user_id = u.id
		          AND ph.created_at < :cutoff
		    WHERE u.status = 'ACTIVE'
		    GROUP BY u.id
		) t
		""", nativeQuery = true)
    int insertWeeklySnapshot(
            @Param("snapshotDate") LocalDate snapshotDate,
            @Param("cutoff") LocalDateTime cutoff
    );
}