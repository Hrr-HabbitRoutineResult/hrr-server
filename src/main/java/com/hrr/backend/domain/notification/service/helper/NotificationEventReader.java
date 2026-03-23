package com.hrr.backend.domain.notification.service.helper;

import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.enums.NotificationTypeName;
import com.hrr.backend.domain.notification.entity.enums.ResourceType;
import com.hrr.backend.domain.notification.repository.NotificationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationEventReader {
    private final NotificationEventRepository eventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<NotificationEvent> findIfExists(ResourceType contextType, Long contextId,
                                                    NotificationTypeName typeName, LocalDate date) {
        return eventRepository.findByContextTypeAndContextIdAndTypeTypeNameAndCreatedDate(
                contextType, contextId, typeName, date);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationEvent tryCreate(NotificationEvent event) {
        return eventRepository.saveAndFlush(event);
    }
}