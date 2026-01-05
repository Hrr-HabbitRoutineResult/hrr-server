package com.hrr.backend.domain.notification.repository;

import com.hrr.backend.domain.notification.entity.NotificationType;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {

    Optional<NotificationType> findByTypeName(NotificationTypeName typeName);
}