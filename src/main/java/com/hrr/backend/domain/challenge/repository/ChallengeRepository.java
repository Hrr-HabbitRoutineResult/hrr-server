package com.hrr.backend.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.challenge.entity.Challenge;

public interface ChallengeRepository extends JpaRepository<Challenge, Long>, ChallengeRepositoryCustom {

}
