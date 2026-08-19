package com.hrr.backend.domain.dm.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import com.hrr.backend.domain.dm.dto.DmMessageSocketDto;
import com.hrr.backend.domain.dm.dto.DmReadSocketDto;
import com.hrr.backend.domain.dm.entity.enums.DmMessageType;
import com.hrr.backend.domain.dm.service.message.DmMessageService;
import com.hrr.backend.domain.dm.service.read.DmReadService;

class DmStompControllerTest {

    private final SimpMessageSendingOperations messagingTemplate = mock(SimpMessageSendingOperations.class);
    private final DmMessageService dmMessageService = mock(DmMessageService.class);
    private final DmReadService dmReadService = mock(DmReadService.class);
    private final DmStompController controller =
            new DmStompController(messagingTemplate, dmMessageService, dmReadService);

    @Test
    void sendMessage_rethrowsServiceFailure_afterLogging() {
        DmMessageSocketDto request = DmMessageSocketDto.builder()
                .conversationId(1L)
                .senderId(2L)
                .content("hello")
                .messageType(DmMessageType.TEXT)
                .build();
        IllegalStateException failure = new IllegalStateException("save failed");
        when(dmMessageService.saveMessage(request)).thenThrow(failure);

        assertThatThrownBy(() -> controller.sendMessage(request)).isSameAs(failure);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void report_rethrowsServiceFailure_afterLogging() {
        DmReadSocketDto.DmReadReport request = DmReadSocketDto.DmReadReport.builder()
                .conversationId(1L)
                .userId(2L)
                .lastReadMessageId(3L)
                .build();
        IllegalStateException failure = new IllegalStateException("read failed");
        org.mockito.Mockito.doThrow(failure).when(dmReadService).report(request);

        assertThatThrownBy(() -> controller.report(request)).isSameAs(failure);
        verify(dmReadService).report(request);
    }
}
