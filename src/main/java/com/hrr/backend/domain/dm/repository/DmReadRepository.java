// src/main/java/com/hrr/backend/domain/dm/repository/DmReadRepository.java
package com.hrr.backend.domain.dm.repository;

import com.hrr.backend.domain.dm.entity.DmMessage;
import com.hrr.backend.domain.dm.entity.DmRead;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DmReadRepository extends JpaRepository<DmRead, Long> {

    Optional<DmRead> findByConversation_IdAndUser_Id(Long conversationId, Long userId);

    boolean existsByConversation_IdAndUser_Id(Long conversationId, Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update DmRead r
           set r.lastReadMessage = :msg,
               r.readAt = :now
         where r.conversation.id = :convId
           and r.user.id = :userId
           and (r.lastReadMessage is null or r.lastReadMessage.id < :msgId)
    """)
    int advanceIfGreater(@Param("convId") Long conversationId,
                         @Param("userId") Long userId,
                         @Param("msgId") Long msgId,
                         @Param("msg") DmMessage msg,
                         @Param("now") LocalDateTime now);
}
