package com.hrr.backend.domain.user.controller;

import com.hrr.backend.domain.user.dto.UserNicknameRequestDto;
import com.hrr.backend.domain.user.dto.UserNicknameResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.service.UserService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 프로필 / 온보딩 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "닉네임 설정",
            description = "회원가입 단계에서 닉네임을 설정합니다. " +
                    "최대 10자까지 입력 가능하며, 중복된 닉네임은 사용할 수 없습니다."
    )
    @PostMapping("/nickname")
    public ApiResponse<UserNicknameResponseDto> setNickname(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserNicknameRequestDto request
    ) {
        User user = userDetails.getUser();
        UserNicknameResponseDto response = userService.setNickname(user, request);

        // result.message = "사용 가능한 닉네임이에요.",  result.nextStep = "MAIN" (LoginStatus.ACTIVE 기준)
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}
