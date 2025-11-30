package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.dto.UserNicknameRequestDto;
import com.hrr.backend.domain.user.dto.UserNicknameResponseDto;
import com.hrr.backend.domain.user.entity.User;

public interface UserService {

    // 사용자 정보 조회
    UserResponseDto.ProfileDto getUserProfile(
            Long userId,
            Long currentUserId
    );
    /**
     * 닉네임 사용 가능 여부만 체크 -> 중복되면 중복 메세지 뜨고록 설정할거
     */
    boolean isNicknameAvailable(String nickname);

    /**
     * 닉네임 저장 + 로그인 상태 변경 + 다음 단계 계산
     */
    UserNicknameResponseDto setNickname(User user, UserNicknameRequestDto request);
}