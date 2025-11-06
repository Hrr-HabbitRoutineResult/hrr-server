package com.hrr.backend.domain.dm.event;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DmReadEventListener {

    private final SimpMessageSendingOperations messagingTemplate;

    @TransactionalEventListener
    public void onUpdated(DmReadUpdatedEvent event) {
        var p = event.getPayload();
        messagingTemplate.convertAndSend(
                "/sub/dm/" + p.getConversationId() + "/read",
                p
        );
    }
}
