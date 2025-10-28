package com.hrr.backend.domain.dm.service.message;

import com.hrr.backend.domain.dm.converter.DmConverter;
import com.hrr.backend.domain.dm.dto.DmMessageSocketDto;
import com.hrr.backend.domain.dm.entity.DmConversation;
import com.hrr.backend.domain.dm.entity.DmMessage;
import com.hrr.backend.domain.dm.repository.DmConversationRepository;
import com.hrr.backend.domain.dm.repository.DmMessageRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DmMessageServiceImpl implements DmMessageService {

    private final DmMessageRepository dmMessageRepository;
    private final DmConversationRepository dmConversationRepository;
    private final UserRepository userRepository;
    private final DmConverter dmConverter;

    @Override
    @Transactional
    public DmMessageSocketDto saveMessage(DmMessageSocketDto dto) {
        // 대화방 확인
        DmConversation conversation = dmConversationRepository.findById(dto.getConversationId())
                .orElseThrow(() -> new GlobalException(ErrorCode.DM_CONVERSATION_NOT_FOUND));

        // 발신자 확인
        User sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // DTO → Entity 변환
        DmMessage message = dmConverter.toMessageEntity(dto, conversation, sender);

        // DB 저장
        DmMessage saved = dmMessageRepository.save(message);

        // Entity → DTO 변환
        return dmConverter.toMessageSocketDto(saved);
    }
}
