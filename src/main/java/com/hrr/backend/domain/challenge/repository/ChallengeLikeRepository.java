package com.hrr.backend.domain.challenge.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeLike;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ChallengeLikeRepository extends JpaRepository<ChallengeLike, Long> {

    // 좋아요 내역 조회 (단건)
    Optional<ChallengeLike> findByUserAndChallenge(User user, Challenge challenge);

    // 좋아요 눌렀는지 여부 조회
    boolean existsByUserAndChallenge(User user, Challenge challenge);

    // 좋아요 내역 삭제
    @Modifying
    @Query("DELETE FROM ChallengeLike cl WHERE cl.user = :user AND cl.challenge = :challenge")
    int deleteByUserAndChallenge(@Param("user") User user,
                                 @Param("challenge") Challenge challenge);
}