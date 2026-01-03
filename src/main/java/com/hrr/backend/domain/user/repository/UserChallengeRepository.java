package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * 사용자 ID와 챌린지 ID로 UserChallenge 조회
     * -> 인증 생성 시 사용자가 해당 챌린지에 참가 중인지 확인
     */
    @Query("SELECT uc FROM UserChallenge uc " +
            "JOIN FETCH uc.user u " +
            "JOIN FETCH uc.challenge c " +
            "WHERE u.id = :userId " +
            "AND c.id = :challengeId")
    Optional<UserChallenge> findByUserIdAndChallengeId(
            @Param("userId") Long userId,
            @Param("challengeId") Long challengeId
    );

    /**
     * 사용자가 참가 중인 모든 챌린지 조회
     */
    @Query("SELECT uc FROM UserChallenge uc " +
            "JOIN FETCH uc.challenge c " +
            "WHERE uc.user.id = :userId")
    List<UserChallenge> findAllByUserId(@Param("userId") Long userId);

    /**
     * 챌린지에 참가 중인 모든 사용자 조회
     */
    @Query("SELECT uc FROM UserChallenge uc " +
            "JOIN FETCH uc.user u " +
            "WHERE uc.challenge.id = :challengeId")
    List<UserChallenge> findAllByChallengeId(@Param("challengeId") Long challengeId);

	// 특정 사용자가 참가 중인 챌린지 정보를 상태에 따라 조회
	List<UserChallenge> findByUserAndStatus(User user, ChallengeJoinStatus challengeJoinStatus);
}
