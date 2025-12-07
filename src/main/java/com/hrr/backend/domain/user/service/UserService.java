package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.domain.user.dto.UserNicknameRequestDto;
import com.hrr.backend.domain.user.dto.UserNicknameResponseDto;
import com.hrr.backend.domain.user.entity.User;

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

    /**
     * 닉네임 사용 가능 여부만 체크 -> 중복되면 중복 메세지 뜨고록 설정할거
     */
    boolean isNicknameAvailable(String nickname);

    /**
     * 닉네임 저장 + 로그인 상태 변경 + 다음 단계 계산
     */
    UserNicknameResponseDto setNickname(User user, UserNicknameRequestDto request);

	/**
	 * 키워드가 닉네임에 포함된 사용자 조회
	 */
	SliceResponseDto<UserResponseDto.ProfileDto> searchChallengers(
			User user,
			String keyword,
			int page,
			int size
	);
}
