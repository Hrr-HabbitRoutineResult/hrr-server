package com.hrr.backend.domain.notification.entity;

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
@Table(name = "notification_type")
public class NotificationType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_name", length = 50, nullable = false, unique = true)
    private String typeName;

    @NotNull
    @Column(name = "default_enabled", nullable = false)
    private boolean defaultEnabled;

    @NotNull
    @Column(name = "is_mandatory", nullable = false)
    private boolean isMandatory;
}
