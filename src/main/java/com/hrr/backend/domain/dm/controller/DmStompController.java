package com.hrr.backend.domain.dm.controller;

import com.hrr.backend.domain.dm.dto.DmMessageSocketDto;
import com.hrr.backend.domain.dm.dto.DmReadSocketDto;
import com.hrr.backend.domain.dm.service.message.DmMessageService;
import com.hrr.backend.domain.dm.service.read.DmReadService;
import com.hrr.backend.global.exception.GlobalException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

@Slf4j
@RequiredArgsConstructor
@Controller
public class DmStompController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final DmMessageService dmMessageService;
    private final DmReadService dmReadService;

    // STOMP 핸들러 내부 예외는 기본적으로 우리 로거를 거치지 않고 Spring STOMP 내부에서만 처리되므로,
    // 각 핸들러에서 직접 잡아서 어떤 메소드에서 발생했는지 명시적으로 남긴다.
    @MessageMapping("/dm.send")
    public void sendMessage(@Valid DmMessageSocketDto messageDto) {
        try {
            // 메시지를 DB에 저장
            DmMessageSocketDto saved = dmMessageService.saveMessage(messageDto);

            // 해당 대화방 구독자에게 메시지 전송
            messagingTemplate.convertAndSend("/sub/dm/" + messageDto.getConversationId(), saved);
        } catch (GlobalException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("[sendMessage] DM 메시지 처리 중 오류가 발생했습니다. conversationId={}",
                    messageDto.getConversationId(), e);
            // 로깅을 추가하더라도 Spring STOMP의 기존 오류 처리 흐름은 유지한다.
            throw e;
        }
    }

    // 인바운드: SEND /pub/dm.read
    @MessageMapping("/dm.read")
    public void report(@Valid DmReadSocketDto.DmReadReport dto) {
        try {
            dmReadService.report(dto);
            // 응답 없음: AFTER_COMMIT 리스너가 /sub/dm/{conversationId}/read로 브로드캐스트
        } catch (GlobalException e) {
            throw e;
        } catch (RuntimeException e) {
            // DmReadServiceImpl에도 동일한 이름의 report가 있어 클래스명까지 명시
            log.error("[report] DM 읽음 처리 중 오류가 발생했습니다. conversationId={}, userId={}, lastReadMessageId={}",
                    dto.getConversationId(), dto.getUserId(), dto.getLastReadMessageId(), e);
            throw e;
        }
    }
}
