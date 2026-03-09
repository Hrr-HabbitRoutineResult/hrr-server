package com.hrr.backend.domain.fcm.repository;

import com.hrr.backend.domain.fcm.entity.FcmToken;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    // 특정 유저와 토큰으로 기존 등록된 FCM 토큰이 있는지 조회
    Optional<FcmToken> findByUserAndToken(User user, String token);

    // 토큰 문자열로 단건 조회 (만료 토큰 비활성화 처리용)
    Optional<FcmToken> findByToken(String token);

    // 특정 유저의 활성화된 FCM 토큰 목록 조회 (푸시 발송용)
    @Query("SELECT t.token FROM FcmToken t WHERE t.user = :user AND t.isActive = true")
    List<String> findAllActiveTokensByUser(@Param("user") User user);
}