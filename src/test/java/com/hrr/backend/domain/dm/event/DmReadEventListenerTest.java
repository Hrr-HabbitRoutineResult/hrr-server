package com.hrr.backend.domain.dm.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import com.hrr.backend.domain.dm.dto.DmReadSocketDto.DmReadEvent;

class DmReadEventListenerTest {

    private final SimpMessageSendingOperations messagingTemplate = mock(SimpMessageSendingOperations.class);
    private final DmReadEventListener listener = new DmReadEventListener(messagingTemplate);

    @Test
    void onUpdated_swallowsBroadcastFailure_afterTransactionCommit() {
        DmReadEvent payload = DmReadEvent.builder()
                .conversationId(1L)
                .userId(2L)
                .lastReadMessageId(3L)
                .readAt(LocalDateTime.of(2026, 8, 16, 12, 0))
                .build();
        DmReadUpdatedEvent event = new DmReadUpdatedEvent(payload);
        String destination = "/sub/dm/1/read";
        doThrow(new IllegalStateException("broker unavailable"))
                .when(messagingTemplate).convertAndSend(destination, payload);

        assertThatCode(() -> listener.onUpdated(event)).doesNotThrowAnyException();

        verify(messagingTemplate).convertAndSend(destination, payload);
    }
}
