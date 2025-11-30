package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.global.response.SliceResponseDto;

public interface UserService {

    // 사용자 정보 조회
    UserResponseDto.ProfileDto getUserProfile(
            Long userId,
            Long currentUserId
    );

    // 참가중인 챌린지 목록 조회 (페이징)
    SliceResponseDto<UserResponseDto.OngoingChallengeDto> getOngoingChallenges(
            Long userId,
            int page,
            int size
    );
    // 내 정보 조회
    UserResponseDto.MyInfoDto getMyInfo(Long userId);
}