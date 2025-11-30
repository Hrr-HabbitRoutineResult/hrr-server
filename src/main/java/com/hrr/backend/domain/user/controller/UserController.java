package com.hrr.backend.domain.user.controller;

import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.service.UserService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description ="사용자 관련 API")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "타인 사용자 기본 정보 조회", description = "특정 사용자의 프로필 정보를 조회합니다.\n")
    public ApiResponse<UserResponseDto.ProfileDto> getUserProfile(
            @PathVariable
            @Parameter(description = "조회할 사용자 ID", example = "999") Long userId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        // 인증된 사용자 ID 추출 (비로그인 시 null)
        Long currentUserId = (customUserDetails != null) ? customUserDetails.getUser().getId() : null;

        UserResponseDto.ProfileDto profile = userService.getUserProfile(userId, currentUserId);
        return ApiResponse.onSuccess(SuccessCode.OK, profile);
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 기본 정보를 조회합니다.")
    public ApiResponse<UserResponseDto.MyInfoDto> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        UserResponseDto.MyInfoDto myInfo = userService.getMyInfo(customUserDetails.getUser().getId());
        return ApiResponse.onSuccess(SuccessCode.OK, myInfo);
    }
}