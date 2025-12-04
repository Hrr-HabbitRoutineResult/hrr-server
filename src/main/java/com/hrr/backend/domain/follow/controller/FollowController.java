package com.hrr.backend.domain.follow.controller;

import com.hrr.backend.domain.follow.dto.FollowActionResponseDto;
import com.hrr.backend.domain.follow.dto.FollowRequestDto;
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

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
@Tag(name = "Follow", description = "팔로우 관련 API")
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "사용자 팔로우", description = "특정 사용자를 팔로우합니다. 공개 계정은 즉시 승인되고, 비공개 계정은 요청 상태가 됩니다.")
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

    @Operation(summary = "사용자 언팔로우", description = "특정 사용자를 언팔로우합니다.")
    @DeleteMapping("/{unfollowedUserId}/unfollow")
    public ApiResponse<FollowResponseDto> unfollowUser(
            @Parameter(description = "언팔로우할 사용자 ID", required = true)
            @PathVariable Long unfollowedUserId,
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        log.info("언팔로우 요청 - currentUserId: {}, unfollowedUserId: {}", currentUserId, unfollowedUserId);

        FollowResponseDto response = followService.unfollowUser(currentUserId, unfollowedUserId);
        return ApiResponse.onSuccess(SuccessCode.UNFOLLOW_SUCCESS, response);
    }

    @Operation(summary = "팔로우 요청 승인", description = "받은 팔로우 요청을 승인합니다.")
    @PostMapping("/follow/{followId}/approve")
    public ApiResponse<FollowActionResponseDto> approveFollowRequest(
            @Parameter(description = "팔로우 ID", required = true)
            @PathVariable Long followId,
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        log.info("팔로우 요청 승인 - currentUserId: {}, followId: {}", currentUserId, followId);

        FollowActionResponseDto response = followService.approveFollowRequest(currentUserId, followId);
        return ApiResponse.onSuccess(SuccessCode.FOLLOW_APPROVED, response);
    }

    @Operation(summary = "팔로우 요청 거절", description = "받은 팔로우 요청을 거절합니다.")
    @DeleteMapping("/follow/{followId}/reject")
    public ApiResponse<FollowActionResponseDto> rejectFollowRequest(
            @Parameter(description = "팔로우 ID", required = true)
            @PathVariable Long followId,
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        log.info("팔로우 요청 거절 - currentUserId: {}, followId: {}", currentUserId, followId);

        FollowActionResponseDto response = followService.rejectFollowRequest(currentUserId, followId);
        return ApiResponse.onSuccess(SuccessCode.FOLLOW_REJECTED, response);
    }

    @Operation(summary = "받은 팔로우 요청 목록 조회", description = "현재 사용자가 받은 대기 중인 팔로우 요청 목록을 조회합니다.")
    @GetMapping("/follow/requests")
    public ApiResponse<List<FollowRequestDto>> getPendingFollowRequests(
            Authentication authentication
    ) {
        Long currentUserId = Long.parseLong(authentication.getName());
        log.info("팔로우 요청 목록 조회 - currentUserId: {}", currentUserId);

        List<FollowRequestDto> response = followService.getPendingFollowRequests(currentUserId);
        return ApiResponse.onSuccess(SuccessCode.OK, response);
    }
}