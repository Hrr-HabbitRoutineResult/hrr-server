package com.hrr.backend.domain.dm.repository;

import com.hrr.backend.domain.dm.entity.DmMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DmMessageRepository extends JpaRepository<DmMessage, Long> {
}
