package com.hrr.backend.domain.notification.entity;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "notification_setting")
public class NotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Builder.Default
    private boolean isChallengeEnabled = true;

    @Builder.Default
    private boolean isVerificationEnabled = true;

    @Builder.Default
    private boolean isFollowEnabled = true;

    @Builder.Default
    private boolean isBadgeEnabled = true;

    // 개별 설정 업데이트
    public void update(Boolean challenge, Boolean verification, Boolean follow, Boolean badge) {
        if (challenge != null) this.isChallengeEnabled = challenge;
        if (verification != null) this.isVerificationEnabled = verification;
        if (follow != null) this.isFollowEnabled = follow;
        if (badge != null) this.isBadgeEnabled = badge;
    }

    // 전체 일시 중단 처리 (모두 끄기)
    public void pauseAll() {
        this.isChallengeEnabled = false;
        this.isVerificationEnabled = false;
        this.isFollowEnabled = false;
        this.isBadgeEnabled = false;
    }

    // 전체 일시 중단 해제 (모두 켜기)
    public void resumeAll() {
        this.isChallengeEnabled = true;
        this.isVerificationEnabled = true;
        this.isFollowEnabled = true;
        this.isBadgeEnabled = true;
    }

    // 전체 일시 중단 판단
    public boolean isAllPaused() {
        return !isChallengeEnabled && !isVerificationEnabled &&
                !isFollowEnabled && !isBadgeEnabled;
    }
}