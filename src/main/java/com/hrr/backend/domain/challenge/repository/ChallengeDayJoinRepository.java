package com.hrr.backend.domain.challenge.repository;

import java.util.List;

import com.hrr.backend.domain.challenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;

public interface ChallengeDayJoinRepository extends JpaRepository<ChallengeDayJoin, Long> {
	List<ChallengeDayJoin> findByChallengeIdIn(List<Long> challengeIds);

}
