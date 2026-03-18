package com.hrr.backend.domain.notification.repository;

import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    // 특정 유저의 알림 설정 정보 조회
    Optional<NotificationSetting> findByUser(User user);

    // 여러 유저의 알림 설정을 한 번에 조회
    List<NotificationSetting> findAllByUserIn(List<User> users);
}