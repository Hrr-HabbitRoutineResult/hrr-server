package com.hrr.backend.domain.notification.repository;

import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.entity.enums.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

    // 특정 리소스에 대해 지정된 시간 이후 알림이 이미 생성되었는지 확인
    boolean existsByContextTypeAndContextIdAndCreatedAtAfter(
            ResourceType contextType,
            Long contextId,
            LocalDateTime dateTime
    );

    // 특정 리소스 + 특정 알림 타입 + 당일 범위로 이벤트가 이미 생성됐는지 확인
    boolean existsByContextTypeAndContextIdAndTypeTypeNameAndCreatedAtBetween(
            ResourceType contextType,
            Long contextId,
            NotificationTypeName typeTypeName,
            LocalDateTime from,
            LocalDateTime to
    );
}