package com.hrr.backend.domain.challenge.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.global.common.enums.Category;

public interface ChallengeRepository extends JpaRepository<Challenge, Long>, ChallengeRepositoryCustom {

	// 사용자가 참가 중인 챌린지들의 카테고리를 조회
	// 없으면 빈 리스트 반환
	@Query("SELECT DISTINCT c.category FROM Challenge c JOIN UserChallenge uc ON uc.challenge = c WHERE uc.user.id = :userId")
	List<Category> findCategoriesByUserId(@Param("userId") Long userId);

}
