package com.hrr.backend.domain.notification.service.helper; // 👈 오타 수정 (hepler -> helper)

import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationType;
import com.hrr.backend.domain.notification.entity.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
@Component
@RequiredArgsConstructor
public class NotificationEventHelper {
    private final NotificationEventReader eventReader;

    public NotificationEvent getOrCreateSharedEvent(ResourceType contextType, Long contextId,
                                                    NotificationType type, String title,
                                                    String message, LocalDate date,
                                                    java.util.function.Supplier<NotificationEvent> creator) {
        return eventReader.upsertAndFind(creator.get());
    }
}
