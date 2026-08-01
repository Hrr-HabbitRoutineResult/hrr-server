package com.hrr.backend.domain.ranking.repository;

import java.time.LocalDate;
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

    // 특정 스냅샷일의 기존 행을 전부 삭제 (같은 트랜잭션에서 upsertWeeklySnapshot 이전에 호출하여 원자적 교체)
    @Modifying
    int deleteBySnapshotDate(LocalDate snapshotDate);

    /** 매주 월요일 00시, 전체 ACTIVE 유저를 대상으로 포인트 내림차순 등수를 계산하여 스냅샷 테이블에 UPSERT*/
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
		        u.id,
		        u.points,
		        COUNT(*) OVER () AS total_count,
		        (SELECT MAX(ph.created_at) FROM point_history ph WHERE ph.user_id = u.id) AS achieved_at
		    FROM user u
		    WHERE u.status = 'ACTIVE'
		) t
		ON DUPLICATE KEY UPDATE
		    ranking = VALUES(ranking),
		    points = VALUES(points),
		    total_user_count = VALUES(total_user_count),
		    achieved_at = VALUES(achieved_at),
		    updated_at = NOW()
		""", nativeQuery = true)
    int upsertWeeklySnapshot(@Param("snapshotDate") LocalDate snapshotDate);
}