package com.hrr.backend.domain.challenge.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeStatics;
import com.hrr.backend.global.common.enums.FavorType;

public interface ChallengeStaticsRepository extends JpaRepository<ChallengeStatics, Integer> {

	// 특정 챌린지에서 특정 선호타입과 값에 해당하는 통계 정보 조회
	Optional<ChallengeStatics> findByChallengeAndFavorTypeAndFavorValue(Challenge challenge, FavorType type, String value);

	List<ChallengeStatics> findByChallenge(Challenge challenge);
}
