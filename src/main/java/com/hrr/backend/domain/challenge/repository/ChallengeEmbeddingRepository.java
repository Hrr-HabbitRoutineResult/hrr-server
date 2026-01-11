package com.hrr.backend.domain.challenge.repository;

import com.hrr.backend.domain.challenge.entity.ChallengeEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChallengeEmbeddingRepository extends JpaRepository<ChallengeEmbedding, Long> {
    Optional<ChallengeEmbedding> findByChallengeId(Long challengeId);
}
