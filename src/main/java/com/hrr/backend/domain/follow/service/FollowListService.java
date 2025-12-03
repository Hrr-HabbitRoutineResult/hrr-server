package com.hrr.backend.domain.follow.service;

import com.hrr.backend.domain.follow.dto.FollowListResponseDto;
import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowListService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    /**
     * 특정 사용자의 팔로워 목록 조회
     * @param userId 조회할 사용자 ID
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return 팔로워 목록
     */
    public List<FollowListResponseDto> getFollowers(Long userId, Long currentUserId) {
        log.info("팔로워 목록 조회 - userId: {}, currentUserId: {}", userId, currentUserId);

        // 조회할 사용자 존재 여부 확인
        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다 - userId: {}", userId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // 팔로워 목록 조회
        List<User> followers = followRepository.findFollowersByUserId(userId);
        log.info("팔로워 목록 조회 완료 - userId: {}, followerCount: {}", userId, followers.size());

        // 팔로워가 없으면 빈 리스트 반환
        if (followers.isEmpty()) {
            return List.of();
        }

        // N+1 방지: 한 번의 쿼리로 현재 사용자가 팔로우 중인 ID 목록 조회
        List<Long> followerIds = followers.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Set<Long> followingIds = new HashSet<>(
                followRepository.findFollowingIdsByFollowerIdAndFollowingIds(currentUserId, followerIds)
        );

        // DTO 변환 (isFollowing 계산)
        return followers.stream()
                .map(follower -> FollowListResponseDto.of(follower, followingIds.contains(follower.getId())))
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 팔로잉 목록 조회
     * @param userId 조회할 사용자 ID
     * @param currentUserId 현재 로그인한 사용자 ID
     * @return 팔로잉 목록
     */
    public List<FollowListResponseDto> getFollowings(Long userId, Long currentUserId) {
        log.info("팔로잉 목록 조회 - userId: {}, currentUserId: {}", userId, currentUserId);

        // 조회할 사용자 존재 여부 확인
        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다 - userId: {}", userId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // 팔로잉 목록 조회
        List<User> followings = followRepository.findFollowingsByUserId(userId);
        log.info("팔로잉 목록 조회 완료 - userId: {}, followingCount: {}", userId, followings.size());

        // 팔로잉이 없으면 빈 리스트 반환
        if (followings.isEmpty()) {
            return List.of();
        }

        // N+1 방지: 한 번의 쿼리로 현재 사용자가 팔로우 중인 ID 목록 조회
        List<Long> followingIds = followings.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        Set<Long> currentUserFollowingIds = new HashSet<>(
                followRepository.findFollowingIdsByFollowerIdAndFollowingIds(currentUserId, followingIds)
        );

        // DTO 변환 (isFollowing 계산)
        return followings.stream()
                .map(following -> FollowListResponseDto.of(following, currentUserFollowingIds.contains(following.getId())))
                .collect(Collectors.toList());
    }
}