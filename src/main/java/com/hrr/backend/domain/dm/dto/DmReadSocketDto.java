package com.hrr.backend.domain.dm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class DmReadSocketDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DmReadReport { // inbound (클 -> 서버)

        @NotNull
        private Long conversationId;

        @NotNull
        private Long userId;

        @NotNull
        @Min(1)
        private Long lastReadMessageId;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DmReadEvent { // outbound (서버 -> 클)

        @NotNull
        private Long conversationId;

        @NotNull
        private Long userId;

        @NotNull
        private Long lastReadMessageId;

        @NotNull
        private LocalDateTime readAt;
    }
}
