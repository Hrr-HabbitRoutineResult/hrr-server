package com.hrr.backend.domain.user.controller;

import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.service.UserService;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description ="사용자 정보 관련 API")
@RestController
@RequestMapping("/api/v1/user") // (API 스펙은 users/인데 사진은 user/네요. 통일 필요)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "타인 사용자 기본 정보 조회", ...)
    public ApiResponse<UserResponseDto> getUserProfile(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetailsImpl userDetails //
    ) {
        UserResponseDto response = userService.getUserProfile(
                userDetails.getUser().getId(),
                userId
        );
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}
