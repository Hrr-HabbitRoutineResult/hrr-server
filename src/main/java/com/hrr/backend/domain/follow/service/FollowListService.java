package com.hrr.backend.domain.follow.service;

import com.hrr.backend.domain.follow.dto.FollowListResponseDto;
import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserBlockRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowListService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;

    /**
     * 특정 사용자의 팔로워 목록 조회 (Slice 페이징)
     * @param userId 조회할 사용자 ID
     * @param currentUserId 현재 로그인한 사용자 ID
     * @param pageable 페이징 정보
     * @return 팔로워 목록 (SliceResponseDto)
     */
    public SliceResponseDto<FollowListResponseDto> getFollowers(Long userId, Long currentUserId, Pageable pageable) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 타인 조회 시: 탈퇴 유저 또는 나를 차단한 유저 조회
        if (!userId.equals(currentUserId)) {
            if (targetUser.isNotActive() || userBlockRepository.existsByBlockerAndBlocked(targetUser, me)) {
                throw new GlobalException(ErrorCode.USER_NOT_FOUND);
            }

            //  내가 차단한 유저의 목록 조회 시 -> 빈 리스트 반환
            if (userBlockRepository.existsByBlockerAndBlocked(me, targetUser)) {
                return new SliceResponseDto<>(new SliceImpl<>(Collections.emptyList(), pageable, false));
            }
        }

        // TODO: 비공개 계정 팔로우 목록 조회 권한 체크 추가 필요
        // - 공개 계정: 누구나 조회 가능
        // - 비공개 계정: 본인 또는 승인된 팔로워만 조회 가능

        // 팔로워 목록 조회 (Slice 페이징)
        Slice<User> followersSlice = followRepository.findFollowersByUserId(userId, currentUserId, pageable);
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
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        User me = userRepository.findById(currentUserId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 타인 조회 시: 탈퇴 유저 또는 나를 차단한 유저 조회 -> 404
        if (!userId.equals(currentUserId)) {
            if (targetUser.isNotActive() || userBlockRepository.existsByBlockerAndBlocked(targetUser, me)) {
                throw new GlobalException(ErrorCode.USER_NOT_FOUND);
            }

            // 내가 차단한 유저의 목록 조회 시 -> 빈 리스트 반환
            if (userBlockRepository.existsByBlockerAndBlocked(me, targetUser)) {
                return new SliceResponseDto<>(new SliceImpl<>(Collections.emptyList(), pageable, false));
            }
        }

        // TODO: 비공개 계정 팔로우 목록 조회 권한 체크 추가 필요
        // - 공개 계정: 누구나 조회 가능
        // - 비공개 계정: 본인 또는 승인된 팔로워만 조회 가능

        // 팔로잉 목록 조회 (Slice 페이징)
        Slice<User> followingsSlice = followRepository.findFollowingsByUserId(userId, currentUserId, pageable);
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
