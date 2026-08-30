package com.hrr.backend.domain.fcm.repository;

import com.hrr.backend.domain.fcm.entity.FcmToken;
import com.hrr.backend.domain.fcm.dto.FcmTokenTargetDto;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    // 특정 유저와 토큰으로 기존 등록된 FCM 토큰 조회
    Optional<FcmToken> findByUserAndToken(User user, String token);

    // 토큰 문자열로 단건 조회 (만료 토큰 비활성화 처리용)
    Optional<FcmToken> findByToken(String token);

    // 특정 유저의 활성화된 FCM 토큰 목록 조회 (단건 발송용)
    @Query("SELECT t.token FROM FcmToken t WHERE t.user = :user AND t.isActive = true")
    List<String> findAllActiveTokensByUser(@Param("user") User user);

    // 응답 순서와 사용자/토큰 레코드를 연결할 수 있도록 내부 ID도 함께 조회한다.
    @Query("SELECT new com.hrr.backend.domain.fcm.dto.FcmTokenTargetDto(t.id, t.user.id, t.token) " +
            "FROM FcmToken t WHERE t.user IN :users AND t.isActive = true")
    List<FcmTokenTargetDto> findAllActiveTokenTargetsByUsers(@Param("users") List<User> users);

    // 특정 토큰을 가진 모든 유저의 레코드를 한 번에 비활성화 (중복 토큰 대응)
    @Modifying(clearAutomatically = true)
    @Query("UPDATE FcmToken t SET t.isActive = false WHERE t.token = :token")
    void deactivateAllByToken(@Param("token") String token);
}
