package com.hrr.backend.domain.follow.service;

import com.hrr.backend.domain.follow.dto.FollowResponseDto;
import com.hrr.backend.domain.follow.entity.Follow;
import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 팔로우
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

        // 이미 팔로우 중인지 확인
        if (followRepository.existsByFollowerIdAndFollowingId(currentUserId, followedUserId)) {
            log.warn("이미 팔로우 중입니다 - followerId: {}, followingId: {}", currentUserId, followedUserId);
            throw new GlobalException(ErrorCode.ALREADY_FOLLOWING);
        }

        // 팔로우 관계 생성
        Follow follow = Follow.builder()
                .follower(currentUser)
                .following(followedUser)
                .build();

        followRepository.save(follow);
        log.info("사용자 팔로우 완료 - followerId: {}, followingId: {}", currentUserId, followedUserId);

        return FollowResponseDto.of("User followed successfully", followedUserId);
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

        // 존재 여부 확인
        userRepository.findById(unfollowedUserId)
                .orElseThrow(() -> {
                    log.warn("언팔로우할 사용자를 찾을 수 없습니다 - userId: {}", unfollowedUserId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // 팔로우 관계 조회
        Follow follow = followRepository.findByFollowerIdAndFollowingId(currentUserId, unfollowedUserId)
                .orElseThrow(() -> {
                    log.warn("팔로우 관계를 찾을 수 없습니다 - followerId: {}, followingId: {}", currentUserId, unfollowedUserId);
                    return new GlobalException(ErrorCode.FOLLOW_NOT_FOUND);
                });

        // 팔로우 관계 삭제
        followRepository.delete(follow);
        log.info("사용자 팔로우 취소 완료 - followerId: {}, followingId: {}", currentUserId, unfollowedUserId);

        return FollowResponseDto.of("User unfollowed successfully", unfollowedUserId);
    }
}