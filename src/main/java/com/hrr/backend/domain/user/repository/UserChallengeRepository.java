package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import com.hrr.backend.domain.user.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.management.relation.Role;
import java.util.Optional;

public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long>, UserChallengeRepositoryCustom {

    // 유저가 특정 챌린지에 이미 참여 중인지 확인
    boolean existsByUserAndChallenge(User user, Challenge challenge);

    // 유저와 챌린지 객체로 내 참여 정보(엔티티) 조회 (인증 여부 체크 시 ID 추출용)
    Optional<UserChallenge> findByUserAndChallenge(User user, Challenge challenge);

    // 역할별 조회 쿼리
    @Query("""
        SELECT uc 
        FROM UserChallenge uc 
        JOIN FETCH uc.user 
        WHERE uc.challenge.id = :challengeId 
        AND uc.role = :role
    """)
    Optional<UserChallenge> findByChallengeIdAndRole(
            @Param("challengeId") Long challengeId,
            @Param("role") UserChallengeRole role
    );
}
