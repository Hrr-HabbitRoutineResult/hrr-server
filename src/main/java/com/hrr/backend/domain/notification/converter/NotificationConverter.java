package com.hrr.backend.domain.notification.converter;

import com.hrr.backend.domain.notification.dto.NotificationResponseDto;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import org.springframework.stereotype.Component;

@Component
public class NotificationConverter {

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