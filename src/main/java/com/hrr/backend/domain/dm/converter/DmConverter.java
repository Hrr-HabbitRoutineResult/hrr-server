package com.hrr.backend.domain.dm.converter;

import com.hrr.backend.domain.dm.dto.DmMessageSocketDto;
import com.hrr.backend.domain.dm.dto.DmReadSocketDto;
import com.hrr.backend.domain.dm.entity.*;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DmConverter {

    public DmMessage toMessageEntity(DmMessageSocketDto dto, DmConversation conversation, User sender) {
        return DmMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(dto.getContent())
                .messageType(dto.getMessageType())
                .clientMessageUuid(dto.getClientMessageUuid())
                .build();
    }

    public DmMessageSocketDto toMessageSocketDto(DmMessage entity) {
        return DmMessageSocketDto.builder()
                .conversationId(entity.getConversation().getId())
                .senderId(entity.getSender().getId())
                .content(entity.getContent())
                .messageType(entity.getMessageType())
                .clientMessageUuid(entity.getClientMessageUuid())
                .build();
    }

    public DmReadSocketDto.DmReadEvent toReadEvent(Long conversationId,
                                                   Long userId,
                                                   Long lastReadMessageId,
                                                   LocalDateTime readAt) {
        return DmReadSocketDto.DmReadEvent.builder()
                .conversationId(conversationId)
                .userId(userId)
                .lastReadMessageId(lastReadMessageId)
                .readAt(readAt)
                .build();
    }
}
