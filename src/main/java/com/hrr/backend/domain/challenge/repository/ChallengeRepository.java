package com.hrr.backend.domain.challenge.repository;

import java.util.List;
import java.util.Optional;

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
}
