package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long> {

    // 유저가 특정 챌린지에 이미 참여 중인지 확인
    boolean existsByUserAndChallenge(User user, Challenge challenge);

    // 인증 생성할 때 유저-챌린지 매핑 조회용
    Optional<UserChallenge> findByUser_IdAndChallenge_Id(Long userId, Long challengeId);
}
