package com.hrr.backend.domain.dm.service.read;

import com.hrr.backend.domain.dm.dto.DmReadSocketDto;

public interface DmReadService {
    void report(DmReadSocketDto.DmReadReport dto);
}
