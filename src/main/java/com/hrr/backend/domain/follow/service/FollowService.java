package com.hrr.backend.domain.follow.service;

import com.hrr.backend.domain.follow.dto.FollowRequestDto;
import com.hrr.backend.domain.follow.dto.FollowResponseDto;
import com.hrr.backend.domain.follow.entity.Follow;
import com.hrr.backend.domain.follow.entity.enums.FollowStatus;
import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 팔로우 (공개 계정: 즉시 승인, 비공개 계정: 요청)
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param followedUserId 팔로우할 사용자 ID
     * @return FollowResponseDto
     */
    @Transactional
    public FollowResponseDto followUser(Long currentUserId, Long followedUserId) {
        log.info("사용자 팔로우 요청 - followerId: {}, followingId: {}", currentUserId, followedUserId);

        // 자기 자신을 팔로우하는지 확인
        if (currentUserId.equals(followedUserId)) {
            log.warn("자기 자신을 팔로우할 수 없습니다 - userId: {}", currentUserId);
            throw new GlobalException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        // 팔로우할 사용자 존재 여부 확인
        User followedUser = userRepository.findById(followedUserId)
                .orElseThrow(() -> {
                    log.warn("팔로우할 사용자를 찾을 수 없습니다 - userId: {}", followedUserId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // 현재 사용자 조회
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.warn("현재 사용자를 찾을 수 없습니다 - userId: {}", currentUserId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // 이미 팔로우 중이거나 요청 중인지 확인
        if (followRepository.existsByFollowerIdAndFollowingId(currentUserId, followedUserId)) {
            log.warn("이미 팔로우 중이거나 요청 중입니다 - followerId: {}, followingId: {}", currentUserId, followedUserId);
            throw new GlobalException(ErrorCode.ALREADY_FOLLOWING);
        }

        // 공개/비공개 계정에 따라 상태 결정
        FollowStatus status = followedUser.getIsPublic() ? FollowStatus.APPROVED : FollowStatus.PENDING;

        // 팔로우 관계 생성
        Follow follow = Follow.builder()
                .follower(currentUser)
                .following(followedUser)
                .status(status)
                .build();

        followRepository.save(follow);

        String message = status == FollowStatus.APPROVED
                ? "User followed successfully"
                : "Follow request sent successfully";

        log.info("사용자 팔로우 완료 - followerId: {}, followingId: {}, status: {}",
                currentUserId, followedUserId, status);

        return FollowResponseDto.of(message, followedUserId, status);
    }

    /**
     * 사용자 팔로우 취소
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param unfollowedUserId 팔로우 취소할 사용자 ID
     * @return FollowResponseDto
     */
    @Transactional
    public FollowResponseDto unfollowUser(Long currentUserId, Long unfollowedUserId) {
        log.info("사용자 팔로우 취소 요청 - followerId: {}, followingId: {}", currentUserId, unfollowedUserId);

        // 대상 사용자가 존재하는지 확인
        userRepository.findById(unfollowedUserId)
                .orElseThrow(() -> {
                    log.warn("팔로우 취소할 사용자를 찾을 수 없습니다 - userId: {}", unfollowedUserId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // 팔로우 관계 조회 (상태 무관)
        Follow follow = followRepository.findByFollowerIdAndFollowingId(currentUserId, unfollowedUserId)
                .orElseThrow(() -> {
                    log.warn("팔로우 관계를 찾을 수 없습니다 - followerId: {}, followingId: {}", currentUserId, unfollowedUserId);
                    return new GlobalException(ErrorCode.FOLLOW_NOT_FOUND);
                });

        // 팔로우 관계 삭제
        followRepository.delete(follow);
        log.info("사용자 팔로우 취소 완료 - followerId: {}, followingId: {}", currentUserId, unfollowedUserId);

        return FollowResponseDto.of("User unfollowed successfully", unfollowedUserId, null);
    }

    /**
     * 팔로우 요청 승인
     * @param currentUserId 현재 로그인한 사용자 ID (요청 받은 사람)
     * @param followId 팔로우 ID
     * @return FollowResponseDto
     */
    @Transactional
    public FollowResponseDto approveFollowRequest(Long currentUserId, Long followId) {
        log.info("팔로우 요청 승인 - userId: {}, followId: {}", currentUserId, followId);

        // 팔로우 요청 조회
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> {
                    log.warn("팔로우 요청을 찾을 수 없습니다 - followId: {}", followId);
                    return new GlobalException(ErrorCode.FOLLOW_REQUEST_NOT_FOUND);
                });

        // 권한 확인: 요청 받은 사람만 승인 가능
        if (!follow.getFollowing().getId().equals(currentUserId)) {
            log.warn("다른 사람의 팔로우 요청을 승인할 수 없습니다 - userId: {}, followId: {}", currentUserId, followId);
            throw new GlobalException(ErrorCode.UNAUTHORIZED_FOLLOW_ACTION);
        }

        // 이미 승인된 요청인지 확인
        if (follow.getStatus() == FollowStatus.APPROVED) {
            log.warn("이미 승인된 팔로우 요청입니다 - followId: {}", followId);
            throw new GlobalException(ErrorCode.ALREADY_APPROVED_FOLLOW);
        }

        // 승인 처리
        follow.approve();
        log.info("팔로우 요청 승인 완료 - followId: {}", followId);

        return FollowResponseDto.of("Follow request approved successfully", followId, FollowStatus.APPROVED);
    }

    /**
     * 팔로우 요청 거절/삭제
     * @param currentUserId 현재 로그인한 사용자 ID (요청 받은 사람)
     * @param followId 팔로우 ID
     * @return FollowResponseDto
     */
    @Transactional
    public FollowResponseDto rejectFollowRequest(Long currentUserId, Long followId) {
        log.info("팔로우 요청 거절 - userId: {}, followId: {}", currentUserId, followId);

        // 팔로우 요청 조회
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> {
                    log.warn("팔로우 요청을 찾을 수 없습니다 - followId: {}", followId);
                    return new GlobalException(ErrorCode.FOLLOW_REQUEST_NOT_FOUND);
                });

        // 권한 확인: 요청 받은 사람만 거절 가능
        if (!follow.getFollowing().getId().equals(currentUserId)) {
            log.warn("다른 사람의 팔로우 요청을 거절할 수 없습니다 - userId: {}, followId: {}", currentUserId, followId);
            throw new GlobalException(ErrorCode.UNAUTHORIZED_FOLLOW_ACTION);
        }

        // 요청 삭제
        followRepository.delete(follow);
        log.info("팔로우 요청 거절 완료 - followId: {}", followId);

        return FollowResponseDto.of("Follow request rejected successfully", followId, null);
    }

    /**
     * 받은 팔로우 요청 목록 조회
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return List<FollowRequestDto>
     */
    public List<FollowRequestDto> getPendingFollowRequests(Long currentUserId) {
        log.info("받은 팔로우 요청 목록 조회 - userId: {}", currentUserId);

        List<Follow> pendingFollows = followRepository.findPendingFollowRequests(currentUserId, FollowStatus.PENDING);

        return pendingFollows.stream()
                .map(FollowRequestDto::from)
                .collect(Collectors.toList());
    }
}