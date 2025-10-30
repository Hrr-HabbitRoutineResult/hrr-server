package com.hrr.backend.domain.dm.service.read;

import com.hrr.backend.domain.dm.converter.DmConverter;
import com.hrr.backend.domain.dm.dto.DmReadSocketDto;
import com.hrr.backend.domain.dm.entity.DmRead;
import com.hrr.backend.domain.dm.event.DmReadUpdatedEvent;
import com.hrr.backend.domain.dm.repository.DmMessageRepository;
import com.hrr.backend.domain.dm.repository.DmReadRepository;
import com.hrr.backend.domain.dm.repository.DmConversationRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DmReadServiceImpl implements DmReadService {

    private final DmReadRepository dmReadRepository;
    private final DmMessageRepository dmMessageRepository;
    private final DmConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;
    private final DmConverter dmConverter;

    @Transactional
    @Override
    public void report(DmReadSocketDto.DmReadReport dto) {
        // 1) 메시지-대화방 소속 검증
        var msg = dmMessageRepository.findByIdAndConversation_Id(
                dto.getLastReadMessageId(), dto.getConversationId()
        ).orElseThrow(() -> new GlobalException(ErrorCode.DM_MESSAGE_NOT_FOUND));

        var now = LocalDateTime.now();

        // 2) 존재하면: 현재값 < 새값일 때만 갱신 (DB가 단조 증가 보장)
        int updated = dmReadRepository.advanceIfGreater(
                dto.getConversationId(), dto.getUserId(), msg.getId(), msg, now
        );
        if (updated > 0) {
            publisher.publishEvent(new DmReadUpdatedEvent(
                    dmConverter.toReadEvent(dto.getConversationId(), dto.getUserId(), msg.getId(), now)
            ));
            return;
        }

        // 3) 없으면 최초 생성
        if (!dmReadRepository.existsByConversation_IdAndUser_Id(dto.getConversationId(), dto.getUserId())) {
            var conv = conversationRepository.findById(dto.getConversationId())
                    .orElseThrow(() -> new GlobalException(ErrorCode.DM_CONVERSATION_NOT_FOUND));
            var user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

            var created = DmRead.builder()
                    .conversation(conv)
                    .user(user)
                    .lastReadMessage(msg)
                    .readAt(now)
                    .build();

            dmReadRepository.save(created);

            publisher.publishEvent(new DmReadUpdatedEvent(
                    dmConverter.toReadEvent(dto.getConversationId(), dto.getUserId(), msg.getId(), now)
            ));
        }
        // 존재하지만 역행/동일 → 아무 것도 안 함(브로드캐스트 X)
    }
}
