package com.hrr.backend.domain.challenge.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeWait;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChallengeWaitRepository extends JpaRepository<ChallengeWait, Long> {

    // 중복 신청 방지용 체크
    boolean existsByUserAndChallenge(User user, Challenge challenge);

    // 취소 시 삭제를 위해 조회
    Optional<ChallengeWait> findByUserAndChallenge(User user, Challenge challenge);
}