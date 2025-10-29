package com.hrr.backend.domain.dm.service.message;

import com.hrr.backend.domain.dm.dto.DmMessageSocketDto;

public interface DmMessageService {
    DmMessageSocketDto saveMessage(DmMessageSocketDto dto);
}
