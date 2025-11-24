package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {
    boolean existsByUserAndChallenge(User user, Challenge challenge);
}
