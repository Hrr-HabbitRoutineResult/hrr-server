package com.hrr.backend.domain.dm.event;

import com.hrr.backend.domain.dm.dto.DmReadSocketDto.DmReadEvent;
import lombok.Value;

@Value
public class DmReadUpdatedEvent {
    DmReadEvent payload;
}