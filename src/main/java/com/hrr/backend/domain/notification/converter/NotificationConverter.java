package com.hrr.backend.domain.notification.converter;

import com.hrr.backend.domain.notification.dto.NotificationResponseDto;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import org.springframework.stereotype.Component;

@Component
public class NotificationConverter {

    // 알림 목록 정보 DTO 변환
    public NotificationResponseDto.InfoDto toInfoDto(NotificationDelivery delivery, String imageUrl) {
        NotificationEvent event = delivery.getEvent();

        return NotificationResponseDto.InfoDto.builder()
                .id(delivery.getId())
                .title(event.getTitle())
                .message(event.getMessage())
                .imageUrl(imageUrl)
                .category(event.getCategory())
                .type(event.getType().getTypeName())
                .targetType(event.getTargetType())
                .targetId(event.getTargetId())
                .contextType(event.getContextType())
                .contextId(event.getContextId())
                .isRead(delivery.getIsRead())
                .createdAt(delivery.getCreatedAt())
                .build();
    }

    public NotificationResponseDto.SettingInfoDto toSettingInfoDto(NotificationSetting setting) {
        return NotificationResponseDto.SettingInfoDto.builder()
                .isAllPaused(setting.isAllPaused())
                .isChallengeEnabled(setting.isChallengeEnabled())
                .isVerificationEnabled(setting.isVerificationEnabled())
                .isFollowEnabled(setting.isFollowEnabled())
                .isBadgeEnabled(setting.isBadgeEnabled())
                .build();
    }
}