package com.hrr.backend.domain.follow.service;

import com.hrr.backend.domain.follow.dto.FollowRequestDto;
import com.hrr.backend.domain.follow.dto.FollowResponseDto;
import com.hrr.backend.domain.follow.entity.Follow;
import com.hrr.backend.domain.follow.entity.enums.FollowStatus;
import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserBlockRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;

    /**
     * 사용자 팔로우 (공개 계정: 즉시 승인, 비공개 계정: 요청)
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param followedUserId 팔로우할 사용자 ID
     * @return FollowResponseDto
     */
    @Retryable(
            value = {OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public FollowResponseDto followUser(Long currentUserId, Long followedUserId) {
        log.info("사용자 팔로우 요청 - followerId: {}, followingId: {}", currentUserId, followedUserId);

        // 자기 자신을 팔로우하는지 확인
        if (currentUserId.equals(followedUserId)) {
            log.warn("자기 자신을 팔로우할 수 없습니다 - userId: {}", currentUserId);
            throw new GlobalException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        // 팔로우할 사용자 조회 및 유효성 체크
        User followedUser = userRepository.findById(followedUserId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 현재 사용자 조회
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> {
                    log.warn("현재 사용자를 찾을 수 없습니다 - userId: {}", currentUserId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // 탈퇴 유저 또는 나를 차단한 유저 체크
        if (followedUser.isNotActive() || userBlockRepository.existsByBlockerAndBlocked(followedUser, currentUser)) {
            throw new GlobalException(ErrorCode.USER_NOT_FOUND);
        }

        // 내가 차단한 유저인 경우 체크 -> 팔로우 불가
        if (userBlockRepository.existsByBlockerAndBlocked(currentUser, followedUser)) {
            throw new GlobalException(ErrorCode.CANNOT_FOLLOW_BLOCKED_USER); // 해당 에러코드 추가 필요
        }

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

        // APPROVED 상태일 때만 카운트 증가 (공개 계정)
        if (status == FollowStatus.APPROVED) {
            currentUser.incrementFollowingCount();
            followedUser.incrementFollowerCount();
        }

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
    @Retryable(
            value = {OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public FollowResponseDto unfollowUser(Long currentUserId, Long unfollowedUserId) {
        log.info("사용자 팔로우 취소 요청 - followerId: {}, followingId: {}", currentUserId, unfollowedUserId);

        // 대상 사용자가 존재하는지 확인
        User unfollowedUser = userRepository.findById(unfollowedUserId)
                .orElseThrow(() -> {
                    log.warn("팔로우 취소할 사용자를 찾을 수 없습니다 - userId: {}", unfollowedUserId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // 현재 사용자 조회
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 팔로우 관계 조회 (상태 무관)
        Follow follow = followRepository.findByFollowerIdAndFollowingId(currentUserId, unfollowedUserId)
                .orElseThrow(() -> {
                    log.warn("팔로우 관계를 찾을 수 없습니다 - followerId: {}, followingId: {}", currentUserId, unfollowedUserId);
                    return new GlobalException(ErrorCode.FOLLOW_NOT_FOUND);
                });

        // APPROVED 상태였을 때만 카운트 감소
        if (follow.getStatus() == FollowStatus.APPROVED) {
            currentUser.decrementFollowingCount();
            unfollowedUser.decrementFollowerCount();
        }

        // 팔로우 관계 삭제
        followRepository.delete(follow);
        log.info("사용자 팔로우 취소 완료 - followerId: {}, followingId: {}", currentUserId, unfollowedUserId);

        return FollowResponseDto.of("User unfollowed successfully", unfollowedUserId, null);
    }

    /**
     * 팔로우 요청 승인
     * @param currentUserId 현재 로그인한 사용자 ID (요청 받은 사람)
     * @param requesterId 팔로우 요청한 사용자 ID
     * @return FollowResponseDto
     */
    @Retryable(
            value = {OptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public FollowResponseDto approveFollowRequest(Long currentUserId, Long requesterId) {
        log.info("팔로우 요청 승인 - currentUserId: {}, requesterId: {}", currentUserId, requesterId);

        // 요청한 사용자 존재 여부 확인
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> {
                    log.warn("요청한 사용자를 찾을 수 없습니다 - userId: {}", requesterId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // 현재 사용자 조회
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // PENDING 상태의 팔로우 요청 조회
        Follow follow = followRepository
                .findByFollowerIdAndFollowingIdAndStatus(requesterId, currentUserId, FollowStatus.PENDING)
                .orElseThrow(() -> {
                    log.warn("팔로우 요청을 찾을 수 없습니다 - requesterId: {}, currentUserId: {}", requesterId, currentUserId);
                    return new GlobalException(ErrorCode.FOLLOW_REQUEST_NOT_FOUND);
                });

        // 승인 처리
        follow.approve();

        // 승인 시점에 카운트 증가
        requester.incrementFollowingCount();
        currentUser.incrementFollowerCount();

        log.info("팔로우 요청 승인 완료 - requesterId: {}, currentUserId: {}", requesterId, currentUserId);

        return FollowResponseDto.of("Follow request approved successfully", requesterId, FollowStatus.APPROVED);
    }

    /**
     * 팔로우 요청 거절/삭제
     * @param currentUserId 현재 로그인한 사용자 ID (요청 받은 사람)
     * @param requesterId 팔로우 요청한 사용자 ID
     * @return FollowResponseDto
     */
    @Transactional
    public FollowResponseDto rejectFollowRequest(Long currentUserId, Long requesterId) {
        log.info("팔로우 요청 거절 - currentUserId: {}, requesterId: {}", currentUserId, requesterId);

        // 요청한 사용자 존재 여부 확인
        userRepository.findById(requesterId)
                .orElseThrow(() -> {
                    log.warn("요청한 사용자를 찾을 수 없습니다 - userId: {}", requesterId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // PENDING 상태의 팔로우 요청 조회
        Follow follow = followRepository
                .findByFollowerIdAndFollowingIdAndStatus(requesterId, currentUserId, FollowStatus.PENDING)
                .orElseThrow(() -> {
                    log.warn("팔로우 요청을 찾을 수 없습니다 - requesterId: {}, currentUserId: {}", requesterId, currentUserId);
                    return new GlobalException(ErrorCode.FOLLOW_REQUEST_NOT_FOUND);
                });

        // 요청 삭제
        followRepository.delete(follow);
        log.info("팔로우 요청 거절 완료 - requesterId: {}, currentUserId: {}", requesterId, currentUserId);

        return FollowResponseDto.of("Follow request rejected successfully", requesterId, null);
    }

    /**
     * 받은 팔로우 요청 목록 조회
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return List<FollowRequestDto>
     */
    public SliceResponseDto<FollowRequestDto> getPendingFollowRequests(Long currentUserId, Pageable pageable) {
        log.info("받은 팔로우 요청 목록 조회 - userId: {}, page: {}, size: {}",
                currentUserId, pageable.getPageNumber(), pageable.getPageSize());

        // Repository를 통해 Slice 형태로 PENDING 요청 조회
        Slice<Follow> pendingFollowsSlice = followRepository.findPendingFollowRequests(
                currentUserId, FollowStatus.PENDING, pageable
        );

        // DTO로 변환
        Slice<FollowRequestDto> dtoSlice = pendingFollowsSlice.map(FollowRequestDto::from);

        return new SliceResponseDto<>(dtoSlice);
    }
}