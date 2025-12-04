package com.hrr.backend.domain.round.repository;

import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoundRecordRepository extends JpaRepository<RoundRecord, Long> {
    // RoundRecordRepository.java
    Optional<RoundRecord> findByUserChallengeAndRoundId(UserChallenge userChallenge, Long roundId);
}