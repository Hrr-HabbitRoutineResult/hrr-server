package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.follow.repository.FollowRepository;
import com.hrr.backend.domain.user.dto.UserNicknameRequestDto;
import com.hrr.backend.domain.user.dto.UserNicknameResponseDto;
import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.LoginStatus;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용 (조회 성능 최적화)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;

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

    // 닉네임 설정 관련

    @Override
    public boolean isNicknameAvailable(String rawNickname) {
        // 클래스 레벨에 @Transactional(readOnly = true)가 있어 메서드 단건 생략 가능
        String nickname = normalize(rawNickname);
        if (nickname.isEmpty()) {
            throw new GlobalException(ErrorCode.NICKNAME_BLANK);
        }

        if (nickname.length() > 10) {
            throw new GlobalException(ErrorCode.NICKNAME_TOO_LONG);
        }

        return !userRepository.existsByNickname(nickname);
    }

    @Override
    @Transactional // 중요: DB 변경(save/update)이 일어나므로 쓰기 트랜잭션 적용
    public UserNicknameResponseDto setNickname(User user, UserNicknameRequestDto request) {
        if (user.getLoginStatus() != LoginStatus.TERMS_DONE) {
            throw new GlobalException(ErrorCode.INVALID_LOGIN_STATUS_FOR_NICKNAME);
        }

        String nickname = normalize(request.getNickname());

        if (!isNicknameAvailable(nickname)) {
            throw new GlobalException(ErrorCode.NICKNAME_DUPLICATED);
        }

        // 닉네임 저장 + 로그인 상태 ACTIVE 전환
        user.updateNickname(nickname);
        user.updateLoginStatus(LoginStatus.ACTIVE);

        // 변경내용 반영
        userRepository.save(user);

        return UserNicknameResponseDto.builder()
                .nickname(nickname)
                .message("사용 가능한 닉네임이에요.")
                .nextStep(user.determineNextStep())   // User 엔티티에 이미 있는 메서드 사용
                .build();
    }

    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }
}