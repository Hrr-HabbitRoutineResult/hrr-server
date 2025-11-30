package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.follow.entity.FollowRepository;
import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final UserChallengeRepository userChallengeRepository;

    @Override
    public UserResponseDto.ProfileDto getUserProfile(Long userId, Long currentUserId) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 팔로잉 여부 확인
        Boolean isFollowing = checkIfFollowing(currentUserId, userId);

        return UserResponseDto.ProfileDto.from(user, isFollowing);
    }

    @Override
    public SliceResponseDto<UserResponseDto.OngoingChallengeDto> getOngoingChallenges(
            Long userId,
            int page,
            int size
    ) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // Pageable 객체 생성 (0-based index)
        Pageable pageable = PageRequest.of(page, size);

        // Repository에서 참가중인 챌린지 조회
        Slice<UserResponseDto.OngoingChallengeDto> slice =
                userChallengeRepository.findOngoingChallengesByUser(user, pageable);

        // SliceResponseDto로 변환하여 반환
        return new SliceResponseDto<>(slice);
    }

    private Boolean checkIfFollowing(Long currentUserId, Long targetUserId) {
        // 비로그인 상태
        if (currentUserId == null) {
            return false;
        }

        // 본인 프로필 조회
        if (currentUserId.equals(targetUserId)) {
            return false;
        }

        // 다른 사람 프로필 조회
        return followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
    }
}