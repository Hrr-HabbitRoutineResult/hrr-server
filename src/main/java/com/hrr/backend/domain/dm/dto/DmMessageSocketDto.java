package com.hrr.backend.domain.dm.dto;

import com.hrr.backend.domain.dm.entity.enums.DmMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "STOMP 인바운드용 DM 메시지 DTO")
public class DmMessageSocketDto {

    @Schema(description = "대화방 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "conversationId는 필수입니다.")
    private Long conversationId;

    @Schema(description = "보내는 사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "senderId는 필수입니다.")
    private Long senderId;

    @Schema(description = "메시지 본문", example = "hello from client", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "content는 비어 있을 수 없습니다.")
    @Size(max = 1000, message = "content는 1000자를 초과할 수 없습니다.")
    private String content;

    @Schema(description = "메시지 타입", example = "TEXT", requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"TEXT","IMAGE","FILE"})
    @NotNull(message = "messageType은 필수입니다.")
    private DmMessageType messageType;

    @Schema(description = "클라이언트 멱등성 키(UUID). 동일 메시지 재시도 시 중복 저장 방지용",
            example = "6f9a0c8b-9c5a-4b05-8d7a-1d6d9f3f0a33", nullable = true)
    @Length(max = 64, message = "clientMessageUuid는 64자를 초과할 수 없습니다.")
    private String clientMessageUuid;
}
