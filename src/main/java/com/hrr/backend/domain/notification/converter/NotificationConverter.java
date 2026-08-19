package com.hrr.backend.domain.notification.converter;

import com.hrr.backend.domain.notification.dto.NotificationResponseDto;
import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationConverter {

    // 알림 목록 정보 DTO 변환
    public NotificationResponseDto.InfoDto toInfoDto(NotificationDelivery delivery, String imageUrl) {
        // 전달받은 배달 객체가 null인 경우
        if (delivery == null) {
            log.error("[toInfoDto] 변환 대상 NotificationDelivery가 null입니다.");
            throw new GlobalException(ErrorCode._INTERNAL_SERVER_ERROR);
        }

        NotificationEvent event = delivery.getEvent();

        // 연결된 이벤트 정보가 없는 경우
        if (event == null) {
            log.error("[toInfoDto] NotificationDelivery에 연결된 NotificationEvent가 없습니다. deliveryId={}",
                    delivery.getId());
            throw new GlobalException(ErrorCode._INTERNAL_SERVER_ERROR);
        }

        // 알림 타입이 없는 경우
        if (event.getType() == null) {
            log.error("[toInfoDto] NotificationEvent에 연결된 NotificationType이 없습니다. eventId={}", event.getId());
            throw new GlobalException(ErrorCode._INTERNAL_SERVER_ERROR);
        }

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
