package com.hrr.backend.domain.user.entity;

import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "follower_count")
    private Long followerCount;

    @Column(name = "following_count")
    private Long followingCount;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_id")
    private AlarmId alarmId;
}