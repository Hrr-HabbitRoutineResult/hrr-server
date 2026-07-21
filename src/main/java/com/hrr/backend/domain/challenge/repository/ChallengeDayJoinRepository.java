package com.hrr.backend.domain.challenge.repository;

import java.util.List;

import com.hrr.backend.domain.challenge.entity.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;

public interface ChallengeDayJoinRepository extends JpaRepository<ChallengeDayJoin, Long> {
	List<ChallengeDayJoin> findByChallengeIdIn(List<Long> challengeIds);

    // 챌린지 수정 기능 - 요일 전체 교체 시 기존 요일 레코드 전부 삭제
    void deleteAllByChallenge(Challenge challenge);
}
