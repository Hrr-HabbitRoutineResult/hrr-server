package com.hrr.backend.domain.notification.service;

import com.hrr.backend.domain.notification.dto.NotificationResponseDto;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.response.SliceResponseDto;

public interface NotificationService {

    // 알림 목록 조회
    SliceResponseDto<NotificationResponseDto.InfoDto> getNotificationList(
            User user, NotificationCategory category, int page, int size);

    // 단일 알림 읽음 처리
    NotificationResponseDto.ReadResultDto markAsRead(User user, Long notificationId);

    // 알림 수신 설정 조회
    NotificationResponseDto.SettingInfoDto getNotificationSettings(User user);
}