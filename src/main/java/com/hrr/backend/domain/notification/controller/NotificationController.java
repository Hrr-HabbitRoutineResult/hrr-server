package com.hrr.backend.domain.notification.controller;

import com.hrr.backend.domain.notification.dto.NotificationResponseDto;
import com.hrr.backend.domain.notification.entity.enums.NotificationCategory;
import com.hrr.backend.domain.notification.service.NotificationService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notification", description = "알림 관련 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("")
    @Operation(summary = "알림 목록 조회", description = "카테고리별 알림 목록을 무한 스크롤(Slice) 형태로 조회합니다.")
    public ApiResponse<SliceResponseDto<NotificationResponseDto.InfoDto>> getNotificationList(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "category", required = false) NotificationCategory category, //
            @Min(1) @RequestParam(name = "page", defaultValue = "1") int page,
            @Min(1) @Max(50) @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        // 1-based index를 0-based로 변환하여 서비스 호출
        SliceResponseDto<NotificationResponseDto.InfoDto> response = notificationService.getNotificationList(
                userDetails.getUser(),
                category,
                page - 1,
                size
        );
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다.")
    public ApiResponse<NotificationResponseDto.ReadResultDto> markAsRead(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long notificationId
    ) {
        NotificationResponseDto.ReadResultDto response =
                notificationService.markAsRead(userDetails.getUser(), notificationId);

        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }

    @GetMapping("/settings")
    @Operation(summary = "알림 수신 설정 조회", description = "유저의 카테고리별 푸시 알림 수신 설정 상태를 조회합니다.")
    public ApiResponse<NotificationResponseDto.SettingInfoDto> getNotificationSettings(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(
                SuccessCode.OK,
                notificationService.getNotificationSettings(userDetails.getUser())
        );
    }

}