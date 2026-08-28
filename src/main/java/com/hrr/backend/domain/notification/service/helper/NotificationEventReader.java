package com.hrr.backend.domain.notification.service.helper;

import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.repository.NotificationEventRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationEventReader {
    private final NotificationEventRepository eventRepository;

    // 호출자의 알림 트랜잭션 안에서 이벤트 생성/조회가 끝나야 delivery 저장과 함께 롤백
    @Transactional(propagation = Propagation.MANDATORY)
    public NotificationEvent upsertAndFind(NotificationEvent event) {
        eventRepository.upsert(event);

        return eventRepository.findByContextTypeAndContextIdAndTypeTypeNameAndCreatedDate(
                        event.getContextType(),
                        event.getContextId(),
                        event.getType().getTypeName(),
                        event.getCreatedDate())
                .orElseThrow(() -> new GlobalException(ErrorCode._INTERNAL_SERVER_ERROR));
    }
}
