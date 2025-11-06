package com.hrr.backend.domain.dm.controller;

import com.hrr.backend.domain.dm.dto.DmMessageSocketDto;
import com.hrr.backend.domain.dm.dto.DmReadSocketDto;
import com.hrr.backend.domain.dm.service.message.DmMessageService;
import com.hrr.backend.domain.dm.service.read.DmReadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@RequiredArgsConstructor
@Controller
public class DmStompController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final DmMessageService dmMessageService;
    private final DmReadService dmReadService;

    @MessageMapping("/dm.send")
    public void sendMessage(@Valid DmMessageSocketDto messageDto) {
        // 메시지를 DB에 저장
        DmMessageSocketDto saved = dmMessageService.saveMessage(messageDto);

        // 해당 대화방 구독자에게 메시지 전송
        messagingTemplate.convertAndSend("/sub/dm/" + messageDto.getConversationId(), saved);
    }

    // 인바운드: SEND /pub/dm.read
    @MessageMapping("/dm.read")
    public void report(@Valid DmReadSocketDto.DmReadReport dto) {
        dmReadService.report(dto);
        // 응답 없음: AFTER_COMMIT 리스너가 /sub/dm/{conversationId}/read로 브로드캐스트
    }
}
