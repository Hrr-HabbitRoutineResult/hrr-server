package com.hrr.backend.domain.notification.service.helper; // 👈 오타 수정 (hepler -> helper)

import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationType;
import com.hrr.backend.domain.notification.entity.enums.ResourceType;
import com.hrr.backend.domain.notification.repository.NotificationEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationEventHelper {
    private final NotificationEventRepository eventRepository;
    private final NotificationEventReader eventReader;

    public NotificationEvent getOrCreateSharedEvent(ResourceType contextType, Long contextId,
                                                    NotificationType type, String title,
                                                    String message, LocalDate date,
                                                    java.util.function.Supplier<NotificationEvent> creator) {
        // 새 트랜잭션에서 조회 시도
        Optional<NotificationEvent> existing = eventReader.findIfExists(contextType, contextId, type.getTypeName(), date);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 새 트랜잭션에서 생성 시도
        try {
            return eventReader.tryCreate(creator.get());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 충돌 시 새 트랜잭션에서 재조회
            Optional<NotificationEvent> persisted = eventReader.findIfExists(contextType, contextId, type.getTypeName(), date);

            if (persisted.isPresent()) {
                return persisted.get(); // 경쟁 상태에서 다른 스레드가 먼저 생성한 경우 정상 반환
            }

            // 재조회 결과가 없다면 유니크 제약 위반이 아닌 다른 무결성 에러이므로 원본 예외를 다시 던짐
            throw e;
        }
    }
}