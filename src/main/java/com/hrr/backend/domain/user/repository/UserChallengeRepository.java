package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {
    /**
 * Checks whether a UserChallenge exists that associates the given user with the given challenge.
 *
 * @param user      the user to check association for
 * @param challenge the challenge to check association for
 * @return          `true` if a UserChallenge exists linking the user and challenge, `false` otherwise
 */
boolean existsByUserAndChallenge(User user, Challenge challenge);
}