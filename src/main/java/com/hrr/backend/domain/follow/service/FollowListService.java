package com.hrr.backend.domain.follow.service;

import com.hrr.backend.domain.follow.dto.FollowListResponseDto;
import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowListService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    /**
     * 특정 사용자의 팔로워 목록 조회 (Slice 페이징)
     * @param userId 조회할 사용자 ID
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param pageable 페이징 정보
     * @return 팔로워 목록 (SliceResponseDto)
     */
    public SliceResponseDto<FollowListResponseDto> getFollowers(Long userId, Long currentUserId, Pageable pageable) {
        log.info("팔로워 목록 조회 - userId: {}, currentUserId: {}, page: {}, size: {}",
                userId, currentUserId, pageable.getPageNumber(), pageable.getPageSize());

        // 조회할 사용자 존재 여부 확인
        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다 - userId: {}", userId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // TODO: 비공개 계정 팔로우 목록 조회 권한 체크 추가 필요
        // - 공개 계정: 누구나 조회 가능
        // - 비공개 계정: 본인 또는 승인된 팔로워만 조회 가능

        // 팔로워 목록 조회 (Slice 페이징)
        Slice<User> followersSlice = followRepository.findFollowersByUserId(userId, pageable);
        log.info("팔로워 목록 조회 완료 - userId: {}, hasNext: {}", userId, followersSlice.hasNext());

        // 팔로워가 없으면 빈 Slice 반환
        if (followersSlice.isEmpty()) {
            return new SliceResponseDto<>(followersSlice.map(user ->
                    FollowListResponseDto.of(user, false)
            ));
        }

        // N+1 방지: 한 번의 쿼리로 현재 사용자가 팔로우 중인 ID 목록 조회
        List<Long> followerIds = followersSlice.getContent().stream()
                .map(User::getId)
                .toList();

        Set<Long> followingIds = new HashSet<>(
                followRepository.findFollowingIdsByFollowerIdAndFollowingIds(currentUserId, followerIds)
        );

        // DTO 변환 (isFollowing 계산)
        Slice<FollowListResponseDto> dtoSlice = followersSlice.map(follower ->
                FollowListResponseDto.of(follower, followingIds.contains(follower.getId()))
        );

        return new SliceResponseDto<>(dtoSlice);
    }

    /**
     * 특정 사용자의 팔로잉 목록 조회 (Slice 페이징)
     * @param userId 조회할 사용자 ID
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param pageable 페이징 정보
     * @return 팔로잉 목록 (SliceResponseDto)
     */
    public SliceResponseDto<FollowListResponseDto> getFollowings(Long userId, Long currentUserId, Pageable pageable) {
        log.info("팔로잉 목록 조회 - userId: {}, currentUserId: {}, page: {}, size: {}",
                userId, currentUserId, pageable.getPageNumber(), pageable.getPageSize());

        // 조회할 사용자 존재 여부 확인
        userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다 - userId: {}", userId);
                    return new GlobalException(ErrorCode.USER_NOT_FOUND);
                });

        // TODO: 비공개 계정 팔로우 목록 조회 권한 체크 추가 필요
        // - 공개 계정: 누구나 조회 가능
        // - 비공개 계정: 본인 또는 승인된 팔로워만 조회 가능

        // 팔로잉 목록 조회 (Slice 페이징)
        Slice<User> followingsSlice = followRepository.findFollowingsByUserId(userId, pageable);
        log.info("팔로잉 목록 조회 완료 - userId: {}, hasNext: {}", userId, followingsSlice.hasNext());

        // 팔로잉이 없으면 빈 Slice 반환
        if (followingsSlice.isEmpty()) {
            return new SliceResponseDto<>(followingsSlice.map(user ->
                    FollowListResponseDto.of(user, false)
            ));
        }

        // N+1 방지: 한 번의 쿼리로 현재 사용자가 팔로우 중인 ID 목록 조회
        List<Long> followingIds = followingsSlice.getContent().stream()
                .map(User::getId)
                .toList();

        Set<Long> currentUserFollowingIds = new HashSet<>(
                followRepository.findFollowingIdsByFollowerIdAndFollowingIds(currentUserId, followingIds)
        );

        // DTO 변환 (isFollowing 계산)
        Slice<FollowListResponseDto> dtoSlice = followingsSlice.map(following ->
                FollowListResponseDto.of(following, currentUserFollowingIds.contains(following.getId()))
        );

        return new SliceResponseDto<>(dtoSlice);
    }
}