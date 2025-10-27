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
@Table(
        name = "fcm_token",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "token"})
)
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "token", length = 512, nullable = false)
    private String token;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    // 활성 여부(로그아웃/토큰만료 시 false)
    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    // FCM 토큰을 다시 활성화
    public void activateToken() {
        // isActive가 false일 때만 모든 업데이트 수행
        if (!this.isActive) {
            this.isActive = true;
            this.registeredAt = LocalDateTime.now();
        }
        // 이미 활성화된 토큰의 경우, registeredAt만 갱신
        else {
            this.registeredAt = LocalDateTime.now(); // 토큰 사용 시점 갱신
        }
    }
}
