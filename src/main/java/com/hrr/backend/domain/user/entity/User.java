package com.hrr.backend.domain.user.entity;

import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.user.entity.enums.Level;
import com.hrr.backend.domain.user.entity.enums.Role;
import com.hrr.backend.domain.user.entity.enums.Status;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "nickname", length = 20, nullable = false)
    private String nickname;

    @NotNull
    @Column(name = "email", length = 255, nullable = false)
    private String email;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @NotNull
    @Column(name = "password", length = 225, nullable = false)
    private String password;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "profile_photo", length = 225)
    private String profilePhoto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private Level level;

    @NotNull // 1. NOT NULL 제약조건 추가 (DB상에서도)
    @Builder.Default // 2. 빌더 기본값 추가
    @Column(name = "follower_count", nullable = false) // 3. nullable = false 추가
    private Long followerCount = 0L;

    @NotNull
    @Builder.Default
    @Column(name = "following_count", nullable = false)
    private Long followingCount = 0L;

    @NotNull
    @Builder.Default
    @Column(name = "points", nullable = false)
    private Long points = 0L;

    @NotNull
    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = true;

    @Builder.Default // 1. 빌더 패턴을 위한 기본값
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationSetting> notificationSettings = new ArrayList<>();
}