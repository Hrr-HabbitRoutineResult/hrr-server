package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.UserResponseDto;

public interface UserService {

    // 사용자 정보 조회
    UserResponseDto.ProfileDto getUserProfile(
            Long userId,
            Long currentUserId
    );

    // 내 정보 조회
    UserResponseDto.MyInfoDto getMyInfo(Long userId);
}