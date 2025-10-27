package com.hrr.backend.domain.fcm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

public class FcmRequest {

    @Getter
    @Schema(description = "FCM 토큰 등록 요청 DTO")
    public static class RegisterDto {
        @Schema(description = "유저 ID", example = "1")
        @NotNull(message = "userId는 필수입니다.")
        private Long userId;

        @Schema(description = "FCM 토큰", example = "d3p1nAbCdEfGhIJKlMnOpQrStUvWxYz123456")
        @NotBlank(message = "fcmToken은 필수입니다.")
        @Length(max = 512, message = "fcmToken은 512자를 초과할 수 없습니다.")
        private String fcmToken;
    }

    @Getter
    @Schema(description = "FCM 토큰 해제 요청 DTO")
    public static class UnregisterDto {
        @Schema(description = "유저 ID", example = "1")
        @NotNull(message = "userId는 필수입니다.")
        private Long userId;

        @Schema(description = "FCM 토큰", example = "d3p1nAbCdEfGhIJKlMnOpQrStUvWxYz123456")
        @NotBlank(message = "fcmToken은 필수입니다.")
        @Length(max = 512, message = "fcmToken은 512자를 초과할 수 없습니다.")
        private String fcmToken;
    }
}
