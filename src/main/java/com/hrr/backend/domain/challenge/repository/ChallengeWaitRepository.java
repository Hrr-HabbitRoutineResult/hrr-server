package com.hrr.backend.domain.challenge.repository;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeWait;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChallengeWaitRepository extends JpaRepository<ChallengeWait, Long> {

    // 중복 신청 방지용 체크
    boolean existsByUserAndChallenge(User user, Challenge challenge);

    // 취소 시 삭제를 위해 조회
    Optional<ChallengeWait> findByUserAndChallenge(User user, Challenge challenge);

    @Query("""
            SELECT cw
            FROM ChallengeWait cw
            JOIN FETCH cw.user u
            JOIN FETCH u.notificationSetting
            WHERE cw.challenge.id = :challengeId
              AND NOT EXISTS (
                    SELECT 1
                    FROM UserChallenge uc
                    WHERE uc.user = u
                      AND uc.challenge = cw.challenge
                      AND uc.status IN :excludedStatuses
              )
            """)
    List<ChallengeWait> findReceiversByChallengeIdExcludingStatuses(
            @Param("challengeId") Long challengeId,
            @Param("excludedStatuses") List<ChallengeJoinStatus> excludedStatuses
    );
}
