package com.hrr.backend.domain.round.repository;

import java.util.Optional;

import com.hrr.backend.domain.round.entity.Round;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoundRepository extends JpaRepository<Round, Long> {

    // 챌린지 ID와 라운드 번호(1, 2, 3...)로 라운드 찾기
    Optional<Round> findByChallengeIdAndRoundNumber(Long challengeId, Integer roundNumber);
}