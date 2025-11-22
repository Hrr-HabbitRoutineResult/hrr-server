package com.hrr.backend.domain.user.controller;

import com.hrr.backend.domain.user.dto.UserTermRequestDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.service.UserTermService;
import com.hrr.backend.global.config.CustomUserDetails;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Terms", description = "사용자 약관 동의 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/terms")
public class UserTermController {

    private final UserTermService userTermService;

    @Operation(summary = "약관 동의 저장", description = "사용자가 동의한 약관을 저장합니다.")
    @PostMapping("/agree")
    public ApiResponse<String> saveUserTerms(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserTermRequestDto.AgreeRequest request
    ) {
        User user = userDetails.getUser();   // CustomUserDetails 에 있는 user 가져오기

        userTermService.saveUserTerms(user, request);

        return ApiResponse.onSuccess(
                SuccessCode.OK,
                "약관 동의 완료 (다음 단계: SIGNUP)"
        );
    }

}
