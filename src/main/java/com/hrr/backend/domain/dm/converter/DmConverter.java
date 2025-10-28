package com.hrr.backend.domain.dm.converter;

import com.hrr.backend.domain.dm.dto.DmMessageSocketDto;
import com.hrr.backend.domain.dm.entity.*;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class DmConverter {

    public DmMessage toMessageEntity(DmMessageSocketDto dto, DmConversation conversation, User sender) {
        return DmMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(dto.getContent())
                .messageType(dto.getMessageType())
                .build();
    }

    public DmMessageSocketDto toMessageSocketDto(DmMessage entity) {
        return DmMessageSocketDto.builder()
                .conversationId(entity.getConversation().getId())
                .senderId(entity.getSender().getId())
                .content(entity.getContent())
                .messageType(entity.getMessageType())
                .build();
    }
}
