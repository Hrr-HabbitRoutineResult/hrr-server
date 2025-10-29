package com.hrr.backend.domain.dm.repository;

import com.hrr.backend.domain.dm.entity.DmConversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DmConversationRepository extends JpaRepository<DmConversation, Long> {
}
