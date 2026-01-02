package com.hrr.backend.domain.notification.repository;

import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    /**
     * 특정 유저의 알림 설정 정보를 조회합니다.
     * 유저와 설정은 1:1 관계이므로 Optional을 반환하여 존재 여부를 안전하게 처리합니다.
     */
    Optional<NotificationSetting> findByUser(User user);
}