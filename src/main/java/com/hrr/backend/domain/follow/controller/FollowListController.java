package com.hrr.backend.domain.follow.controller;

import com.hrr.backend.domain.follow.dto.FollowListResponseDto;
import com.hrr.backend.domain.follow.service.FollowListService;
import com.hrr.backend.global.response.ApiResponse;
import com.hrr.backend.global.response.SuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@Tag(name = "Follow List", description = "팔로워/팔로잉 목록 조회 API")
public class FollowListController {

    private final FollowListService followListService;

    @Operation(summary = "팔로워 목록 조회", description = "특정 사용자를 팔로우하는 사용자 목록을 조회합니다.")
    @GetMapping("/{userId}/follower")
    public ApiResponse<List<FollowListResponseDto>> getFollowers(
            @Parameter(description = "조회할 사용자 ID", required = true)
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        log.info("팔로워 목록 조회 요청 - userId: {}, currentUserId: {}", userId, currentUserId);

        List<FollowListResponseDto> followers = followListService.getFollowers(userId, currentUserId);
        return ApiResponse.onSuccess(SuccessCode.OK, followers);
    }

    @Operation(summary = "팔로잉 목록 조회", description = "특정 사용자가 팔로우하는 사용자 목록을 조회합니다.")
    @GetMapping("/{userId}/following")
    public ApiResponse<List<FollowListResponseDto>> getFollowings(
            @Parameter(description = "조회할 사용자 ID", required = true)
            @PathVariable Long userId,
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        log.info("팔로잉 목록 조회 요청 - userId: {}, currentUserId: {}", userId, currentUserId);

        List<FollowListResponseDto> followings = followListService.getFollowings(userId, currentUserId);
        return ApiResponse.onSuccess(SuccessCode.OK,followings);
    }
}