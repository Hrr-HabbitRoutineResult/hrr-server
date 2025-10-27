package com.hrr.backend.domain.fcm.controller;

import com.hrr.backend.domain.fcm.dto.FcmRequest;
import com.hrr.backend.domain.fcm.service.FcmService;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "FCM", description = "FCM 관련 API")
@RestController
@RequestMapping("/api/v1/fcm")
@RequiredArgsConstructor
public class FcmController {

    private final FcmService fcmService;

    @PostMapping("/token")
    @Operation(summary = "FCM 토큰 등록", description = "유저의 FCM 토큰을 서버에 등록합니다.")
    public ApiResponse<Void> registerFcmToken(@Valid @RequestBody FcmRequest.RegisterDto request) {
        fcmService.registerFcmToken(request);
        return ApiResponse.onSuccess(SuccessCode.OK, null);
    }
}
