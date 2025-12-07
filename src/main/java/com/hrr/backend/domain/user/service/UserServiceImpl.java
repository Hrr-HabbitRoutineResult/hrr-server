package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.*;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.LoginStatus;
import com.hrr.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 (조회 성능 최적화)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final UserChallengeRepository userChallengeRepository;

    // 프로필 조회 관련

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

    // 닉네임 유효성 검사 (중복 체크)
    @Override
    public boolean isNicknameAvailable(String rawNickname) {
        String nickname = normalize(rawNickname);
        return !userRepository.existsByNickname(nickname);
    }

    @Override
    public UserResponseDto.MyInfoDto getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        return UserResponseDto.MyInfoDto.from(user);
    }

    // 닉네임 설정
    @Override
    @Transactional
    public UserNicknameResponseDto setNickname(User user, UserNicknameRequestDto request) {

        // 온보딩 상태 검증
        if (user.getLoginStatus() != LoginStatus.TERMS_DONE) {
            throw new GlobalException(ErrorCode.INVALID_LOGIN_STATUS_FOR_NICKNAME);
        }

        String nickname = normalize(request.getNickname());

        // 닉네임 중복 검사만 수행 (빈값/길이는 DTO Validation에서 수행)
        if (!isNicknameAvailable(nickname)) {
            throw new GlobalException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // 닉네임 저장 + ACTIVE 전환
        user.updateNickname(nickname);
        user.updateLoginStatus(LoginStatus.ACTIVE);
        userRepository.save(user);

        return UserNicknameResponseDto.builder()
                .nickname(nickname)
                .message("사용 가능한 닉네임이에요.")
                .nextStep(user.determineNextStep())
                .build();
    }

    // 사용자 기본 정보 수정
    @Override
    @Transactional
    public UpdateUserInfoResponseDto updateUserInfo(Long userId, UpdateUserInfoRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 닉네임이 제공된 경우 중복 검사 및 업데이트
        if (requestDto.getNickname() != null && !requestDto.getNickname().equals(user.getNickname())) {
            if (userRepository.existsByNickname(requestDto.getNickname())) {
                throw new GlobalException(ErrorCode.NICKNAME_DUPLICATED);
            }
            user.updateNickname(requestDto.getNickname());
        }

        // 프로필 이미지가 제공된 경우 업데이트
        if (requestDto.getProfileImage() != null) {
            user.updateProfileImage(requestDto.getProfileImage());
        }

        // 프로필 공개 여부가 제공된 경우 업데이트
        if (requestDto.getIsPublic() != null) {
            user.updateIsPublic(requestDto.getIsPublic());
        }

        return UpdateUserInfoResponseDto.from(user);
    }

    private String normalize(String raw) {
        if (raw == null) return "";
        return raw.trim();
    }
}