package com.hrr.backend.domain.notification.entity;

import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.notification.entity.enums.ResourceType;
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
@Table(name = "notification_event")
public class NotificationEvent extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private NotificationType type;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20, nullable = false)
    private NotificationCategory category;

    // 이동 대상
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private ResourceType targetType;

    @NotNull
    @Column(name = "target_id", nullable = false)
    private Long targetId;

    // 발생 원인
    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", length = 30)
    private ResourceType contextType;

    @Column(name = "context_id")
    private Long contextId;

    @NotNull @Column(name = "title", length = 100, nullable = false)
    private String title;

    @NotNull @Column(name = "message", length = 255, nullable = false)
    private String message;
}

