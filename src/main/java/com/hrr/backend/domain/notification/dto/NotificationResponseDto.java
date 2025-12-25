package com.hrr.backend.domain.notification.dto;

import com.hrr.backend.domain.notification.entity.NotificationDelivery;
import com.hrr.backend.domain.notification.entity.NotificationEvent;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

public class NotificationResponseDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "알림 목록 정보 DTO")
    public static class InfoDto {
        @Schema(description = "알림 배달 ID", example = "1")
        private Long id;

        @Schema(description = "알림 제목", example = "내 인증에 댓글이 달렸어요")
        private String title;

        @Schema(description = "알림 메시지", example = "어 저랑 같은 문제 풀었는데...")
        private String message;

        @Schema(description = "알림 카테고리", example = "VERIFICATION")
        private NotificationCategory category;

        @Schema(description = "이동 대상 타입", example = "COMMENT")
        private String targetType;

        @Schema(description = "이동 대상 ID", example = "501")
        private Long targetId;

        @Schema(description = "읽음 여부 (true: 읽음, false: 안읽음-NEW)", example = "false")
        private Boolean isRead;

        @Schema(description = "알림 발생 시간", example = "2025-12-25T14:30:00")
        private LocalDateTime createdAt;

        public static InfoDto from(NotificationDelivery delivery) {
            NotificationEvent event = delivery.getEvent();
            return InfoDto.builder()
                    .id(delivery.getId())
                    .title(event.getTitle())
                    .message(event.getMessage())
                    .category(event.getCategory())
                    .targetType(event.getTargetType().name())
                    .targetId(event.getTargetId())
                    .isRead(delivery.getIsRead())
                    .createdAt(delivery.getCreatedAt())
                    .build();
        }
    }
}