package com.hrr.backend.domain.dm.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DmReadEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    @TransactionalEventListener
    public void onUpdated(DmReadUpdatedEvent event) {
        var p = event.getPayload();
        try {
            messagingTemplate.convertAndSend(
                    "/sub/dm/" + p.getConversationId() + "/read",
                    p
            );
        } catch (Exception e) {
            // 트랜잭션은 이미 커밋된 뒤이므로 읽음 처리 자체는 성공. 실시간 브로드캐스트만 실패한 경우.
            log.error("[onUpdated] 읽음 상태 브로드캐스트 실패. conversationId={}", p.getConversationId(), e);
        }
    }
}
