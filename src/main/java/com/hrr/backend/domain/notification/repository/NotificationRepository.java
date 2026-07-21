package com.hrr.backend.domain.notification.repository;

import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.entity.enums.ResourceType;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationDelivery, Long> {

    @Query("SELECT nd FROM NotificationDelivery nd " +
            "JOIN FETCH nd.event e " +
            "JOIN FETCH e.type " +
            "WHERE nd.receiver = :user " +
            "AND (:category IS NULL OR e.category = :category) " +
            "ORDER BY nd.createdAt DESC") // 최신순 정렬
    Slice<NotificationDelivery> findMyNotifications(
            @Param("user") User user,
            @Param("category") NotificationCategory category,
            Pageable pageable
    );

    @Query("SELECT COUNT(nd) > 0 FROM NotificationDelivery nd " +
            "JOIN nd.event e " +
            "WHERE nd.receiver = :user " +
            "AND e.contextType = :contextType " +
            "AND e.contextId = :contextId " +
            "AND e.type.typeName IN :typeNames")
    boolean existsResponseNotification(
            @Param("user") User user,
            @Param("contextType") ResourceType contextType,
            @Param("contextId") Long contextId,
            @Param("typeNames") List<NotificationTypeName> typeNames
    );

    // 읽지 않은 알림이 존재하는지 확인
    boolean existsByReceiverAndIsReadFalse(User user);

}
