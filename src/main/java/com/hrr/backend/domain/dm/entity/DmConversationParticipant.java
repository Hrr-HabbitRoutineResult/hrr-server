package com.hrr.backend.domain.dm.entity;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "dm_conversation_participant",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"conversation_id", "user_id"}) },
        indexes = {
                @Index(name = "ix_dm_part_conv_user", columnList = "conversation_id,user_id")
        }
)
public class DmConversationParticipant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private DmConversation conversation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Builder.Default
    @Column(name = "is_muted", nullable = false)
    private Boolean isMuted = false;

    @NotNull
    @Builder.Default
    @Column(name = "is_blocked", nullable = false)
    private Boolean isBlocked = false;

}
