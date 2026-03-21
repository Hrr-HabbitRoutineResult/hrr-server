package com.hrr.backend.domain.notification.repository;

import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.entity.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

    // 특정 리소스 + 특정 알림 타입 + 특정 날짜로 이벤트가 이미 생성됐는지 확인
    boolean existsByContextTypeAndContextIdAndTypeTypeNameAndCreatedDate(
            ResourceType contextType,
            Long contextId,
            NotificationTypeName typeTypeName,
            LocalDate createdDate
    );

    // 특정 리소스에 대해 오늘 이후로 알림이 생성되었는지 확인
    boolean existsByContextTypeAndContextIdAndCreatedDateGreaterThanEqual(
            ResourceType contextType,
            Long contextId,
            LocalDate date
    );
}