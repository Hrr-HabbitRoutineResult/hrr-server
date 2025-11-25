package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.user.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {

    // 유저가 특정 챌린지에 이미 참여 중인지 확인
    boolean existsByUserAndChallenge(User user, Challenge challenge);
}
