package com.hrr.backend.domain.notification.repository;

import com.hrr.backend.domain.notification.entity.NotificationEvent;

public interface NotificationEventUpsertRepository {
    void upsert(NotificationEvent event);
}
