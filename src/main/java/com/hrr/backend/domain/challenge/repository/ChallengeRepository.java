package com.hrr.backend.domain.challenge.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.global.common.enums.Category;

public interface ChallengeRepository extends JpaRepository<Challenge, Long>, ChallengeRepositoryCustom {

	// 사용자가 참가 중인 챌린지들의 카테고리를 조회
	// 없으면 빈 리스트 반환
	@Query("SELECT DISTINCT c.category FROM Challenge c JOIN UserChallenge uc ON uc.challenge = c WHERE uc.user.id = :userId")
	List<Category> findCategoriesByUserId(@Param("userId") Long userId);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE Challenge c SET c.likeCount = c.likeCount + 1 WHERE c.id = :id")
	void increaseLikeCount(@Param("id") Long id);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE Challenge c SET c.likeCount = c.likeCount - 1 WHERE c.id = :id AND c.likeCount > 0")
	void decreaseLikeCount(@Param("id") Long id);

	// 요일 정보(ChallengeDays)까지 한 번에 가져오는 Fetch Join 쿼리
	@Query("SELECT c FROM Challenge c LEFT JOIN FETCH c.challengeDays WHERE c.id = :id")
	Optional<Challenge> findByIdWithDays(@Param("id") Long id);

	List<Challenge> findAllByStartDateGreaterThanEqualAndStartDateLessThan(
			LocalDateTime startInclusive,
			LocalDateTime endExclusive
	);

	/**
	 * 기준 시각 이전(포함)의 UPCOMING 챌린지 ID 조회
	 * - 스케줄러가 로그용으로 먼저 조회할 때 사용
	 */
	@Query("SELECT c.id FROM Challenge c " +
			"WHERE c.status = :currentStatus AND c.startDate <= :referenceTime")
	List<Long> findIdsToStart(
			@Param("currentStatus") ChallengeStatus currentStatus,
			@Param("referenceTime") LocalDateTime referenceTime
	);

	/**
	 * 기준 시각 이전(포함)의 UPCOMING 챌린지를 ONGOING으로 일괄 변경
	 */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Challenge c SET c.status = :targetStatus " +
			"WHERE c.status = :currentStatus AND c.startDate <= :referenceTime")
	int updateChallengeStatusToOngoing(
			@Param("targetStatus") ChallengeStatus targetStatus,
			@Param("currentStatus") ChallengeStatus currentStatus,
			@Param("referenceTime") LocalDateTime referenceTime
	);

	// 사용자가 참가 중인 챌린지의 총 개수를 조회
	@Query("SELECT COUNT(uc) FROM UserChallenge uc WHERE uc.user.id = :userId AND uc.status = :status")
	Long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ChallengeJoinStatus status);

	// 참가 인원을 atomic하게 감소
	@Modifying
	@Query("UPDATE Challenge c " +
		"SET c.currentParticipants = c.currentParticipants - 1 " +
		"WHERE c.id = :id AND c.currentParticipants > 0")
	int decreaseCurrentParticipantCount(@Param("id") Long id);

    @Query("""
    select c.id
    from Challenge c
    where c.id in :ids
      and c.status <> :finished
    """)
    List<Long> findNotFinishedIds(
            @Param("ids") List<Long> ids,
            @Param("finished") ChallengeStatus finished
    );


}
