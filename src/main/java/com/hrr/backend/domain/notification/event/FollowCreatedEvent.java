package com.hrr.backend.domain.notification.event;

import com.hrr.backend.domain.user.entity.User;

/**
 * 팔로우 성립 알림 이벤트를 위한 레코드
 */
public record FollowCreatedEvent(
        User actor,
        User receiver
) {
}
