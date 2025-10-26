package com.hrr.backend.domain.fcm.entity;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
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
@Table(name = "fcm_token")
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "token", length = 255, nullable = false)
    private String token;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    // 활성 여부(로그아웃/토큰만료 시 false)
    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
