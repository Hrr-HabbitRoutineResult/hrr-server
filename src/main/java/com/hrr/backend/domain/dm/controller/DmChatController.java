package com.hrr.backend.domain.dm.controller;

import com.hrr.backend.domain.dm.dto.DmMessageSocketDto;
import com.hrr.backend.domain.dm.service.message.DmMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class DmChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final DmMessageService dmMessageService;

    @MessageMapping("/dm.send")
    public void sendMessage(DmMessageSocketDto messageDto) {
        // 메시지를 DB에 저장
        DmMessageSocketDto saved = dmMessageService.saveMessage(messageDto);

        // 해당 대화방 구독자에게 메시지 전송
        messagingTemplate.convertAndSend("/sub/dm/" + messageDto.getConversationId(), saved);
    }
}
