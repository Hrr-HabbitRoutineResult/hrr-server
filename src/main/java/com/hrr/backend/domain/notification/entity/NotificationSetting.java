package com.hrr.backend.domain.notification.entity;

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
        name = "notification_setting",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "type_id"})
        },
        indexes = {
                @Index(name = "idx_notification_setting_user", columnList = "user_id"),
                @Index(name = "idx_notification_setting_type", columnList = "type_id")
        }
)
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id")
    private NotificationType type;

    @NotNull
    @Column(name = "settings", nullable = false)
    private boolean settings;
}
