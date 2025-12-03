package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserChallengeRepository extends JpaRepository<UserChallenge, Long>, UserChallengeRepositoryCustom {

    // 유저가 특정 챌린지에 이미 참여 중인지 확인
    boolean existsByUserAndChallenge(User user, Challenge challenge);

    // 유저와 챌린지 객체로 내 참여 정보(엔티티) 조회 (이미 객체가 있을 때 사용)
    Optional<UserChallenge> findByUserAndChallenge(User user, Challenge challenge);

    // 인증 생성할 때 유저-챌린지 매핑 조회용 (ID만 알 때 사용)
    Optional<UserChallenge> findByUser_IdAndChallenge_Id(Long userId, Long challengeId);

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