package com.hrr.backend.domain.dm.entity;

import com.hrr.backend.domain.dm.entity.enums.DeliveryStatus;
import com.hrr.backend.domain.dm.entity.enums.DmMessageType;
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
        name = "dm_message",
        indexes = {
                @Index(name = "ix_dm_msg_conv_id", columnList = "conversation_id,id")
        }
)
public class DmMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private DmConversation conversation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Lob
    @Column(name = "content")
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", length = 10, nullable = false)
    private DmMessageType messageType;

    // 클라이언트 중복 전송 방지용
    @Column(name = "client_message_uuid", length = 64, unique = true)
    private String clientMessageUuid;

    @NotNull
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", length = 12, nullable = false)
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENT;

}
