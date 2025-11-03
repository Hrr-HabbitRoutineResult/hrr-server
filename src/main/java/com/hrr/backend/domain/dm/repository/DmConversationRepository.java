// com.hrr.backend.domain.dm.repository.DmConversationRepository.java
package com.hrr.backend.domain.dm.repository;

import com.hrr.backend.domain.dm.entity.DmConversation;
import com.hrr.backend.domain.dm.entity.DmMessage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DmConversationRepository extends JpaRepository<DmConversation, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update DmConversation c
           set c.lastMessage = :msg
         where c.id = :convId
           and (c.lastMessage is null or c.lastMessage.id < :msgId)
    """)
    int advanceLastMessageIfGreater(@Param("convId") Long conversationId,
                                    @Param("msgId") Long msgId,
                                    @Param("msg") DmMessage msg);
}
