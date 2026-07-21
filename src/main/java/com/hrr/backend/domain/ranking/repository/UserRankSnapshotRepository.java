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
            "JOIN FETCH s.user " +
            "WHERE s.snapshotDate = :snapshotDate " +
            "ORDER BY s.ranking ASC")
    List<UserRankSnapshot> findTopByRanking(@Param("snapshotDate") LocalDate snapshotDate, Pageable pageable);

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

    /** 매주 월요일 00시, 전체 ACTIVE 유저를 대상으로 포인트 내림차순 등수를 계산하여 스냅샷 테이블에 UPSERT*/
    @Modifying
    @Query(value = """
		INSERT INTO user_rank_snapshot (user_id, ranking, points, total_user_count, snapshot_date, created_at, updated_at)
		SELECT
		    t.id,
		    RANK() OVER (ORDER BY t.points DESC, t.id ASC),
		    t.points,
		    t.total_count,
		    :snapshotDate,
		    NOW(),
		    NOW()
		FROM (
		    SELECT u.id, u.points, COUNT(*) OVER () AS total_count
		    FROM user u
		    WHERE u.status = 'ACTIVE'
		) t
		ON DUPLICATE KEY UPDATE
		    ranking = VALUES(ranking),
		    points = VALUES(points),
		    total_user_count = VALUES(total_user_count),
		    updated_at = NOW()
		""", nativeQuery = true)
    int upsertWeeklySnapshot(@Param("snapshotDate") LocalDate snapshotDate);
}