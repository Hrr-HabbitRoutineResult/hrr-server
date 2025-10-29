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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
        // 대화방/발신자 확인
        var conversation = dmConversationRepository.findById(dto.getConversationId())
                .orElseThrow(() -> new GlobalException(ErrorCode.DM_CONVERSATION_NOT_FOUND));
        var sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 멱등성 선조회
        if (StringUtils.hasText(dto.getClientMessageUuid())) {
            var existed = dmMessageRepository.findByClientMessageUuid(dto.getClientMessageUuid());
            if (existed.isPresent()) {
                return dmConverter.toMessageSocketDto(existed.get());
            }
        }

        // 신규 저장 시도
        var entity = dmConverter.toMessageEntity(dto, conversation, sender);
        try {
            var saved = dmMessageRepository.save(entity);
            return dmConverter.toMessageSocketDto(saved);
        } catch (DataIntegrityViolationException e) {
            // 동시 저장으로 UNIQUE 충돌 → 재조회 후 기존 레코드 반환
            if (StringUtils.hasText(dto.getClientMessageUuid())) {
                return dmMessageRepository.findByClientMessageUuid(dto.getClientMessageUuid())
                        .map(dmConverter::toMessageSocketDto)
                        .orElseThrow(() -> e);
            }
            throw e;
        }
    }
}
