package com.hrr.backend.domain.dm.dto;

import com.hrr.backend.domain.dm.entity.enums.DmMessageType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DmMessageSocketDto {

    private Long conversationId; // 대화방 ID
    private Long senderId;       // 보낸 사람 ID
    private String content;      // 메시지 내용
    private DmMessageType messageType; // 메시지 타입(TEXT/IMAGE 등)
}
