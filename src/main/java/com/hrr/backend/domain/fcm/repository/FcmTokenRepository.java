package com.hrr.backend.domain.fcm.repository;

import com.hrr.backend.domain.fcm.entity.FcmToken;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    // 특정 유저와 토큰으로 기존 등록된 FCM 토큰이 있는지 조회
    Optional<FcmToken> findByUserAndToken(User user, String token);

}
