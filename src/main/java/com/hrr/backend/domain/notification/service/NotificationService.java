package com.hrr.backend.domain.notification.service;

import com.hrr.backend.domain.notification.dto.NotificationResponseDto;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.response.SliceResponseDto;

public interface NotificationService {
    SliceResponseDto<NotificationResponseDto.InfoDto> getNotificationList(
            User user, NotificationCategory category, int page, int size);
}