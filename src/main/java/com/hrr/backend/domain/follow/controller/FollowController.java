package com.hrr.backend.domain.follow.controller;

import com.hrr.backend.domain.follow.dto.FollowResponseDto;
import com.hrr.backend.domain.follow.service.FollowService;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@Tag(name = "Follow", description = "팔로우 관련 API")
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "사용자 팔로우", description = "특정 사용자를 팔로우합니다.")
    @PostMapping("/{followedUserId}/follow")
    public ApiResponse<FollowResponseDto> followUser(
            @Parameter(description = "팔로우할 사용자 ID", required = true)
            @PathVariable Long followedUserId,
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        log.info("팔로우 요청 - currentUserId: {}, followedUserId: {}", currentUserId, followedUserId);

        FollowResponseDto response = followService.followUser(currentUserId, followedUserId);
        return ApiResponse.onSuccess(SuccessCode.FOLLOW_SUCCESS, response);
    }

    @Operation(summary = "사용자 팔로우 취소", description = "특정 사용자 팔로우를 취소합니다.")
    @DeleteMapping("/{unfollowedUserId}/unfollow")
    public ApiResponse<FollowResponseDto> unfollowUser(
            @Parameter(description = "팔로우 취소할 사용자 ID", required = true)
            @PathVariable Long unfollowedUserId,
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        log.info("팔로우 취소 요청 - currentUserId: {}, unfollowedUserId: {}", currentUserId, unfollowedUserId);

        FollowResponseDto response = followService.unfollowUser(currentUserId, unfollowedUserId);
        return ApiResponse.onSuccess(SuccessCode.UNFOLLOW_SUCCESS, response);
    }
}