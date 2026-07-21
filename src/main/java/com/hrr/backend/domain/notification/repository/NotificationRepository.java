package com.hrr.backend.domain.notification.repository;

import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationDelivery, Long> {

    @Query("SELECT nd FROM NotificationDelivery nd " +
            "JOIN FETCH nd.event e " +
            "WHERE nd.receiver = :user " +
            "AND (:category IS NULL OR e.category = :category) " +
            "ORDER BY nd.createdAt DESC") // 최신순 정렬
    Slice<NotificationDelivery> findMyNotifications(
            @Param("user") User user,
            @Param("category") NotificationCategory category,
            Pageable pageable
    );

    // 읽지 않은 알림이 존재하는지 확인
    boolean existsByReceiverAndIsReadFalse(User user);

}
