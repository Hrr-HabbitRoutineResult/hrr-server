package com.hrr.backend.domain.recommendation.repository;

import com.hrr.backend.domain.recommendation.entity.RecommendationResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {
}
