package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.*;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;

public interface UserService {

    // 사용자 정보 조회
    UserResponseDto.ProfileDto getUserProfile(
            Long userId,
            User currentUser
    );

    // 참가중인 챌린지 목록 조회 (페이징)
    SliceResponseDto<UserResponseDto.OngoingChallengeDto> getOngoingChallenges(
            Long userId,
            User currentUser,
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

    /**
     * 찜한 챌린지 목록 조회 (페이징)
     */
    SliceResponseDto<UserResponseDto.LikedChallengeDto> getMarkedChallenges(
            Long userId,      
            int page,
            int size
    );

    /**
     * 종료한 챌린지 목록 조회 (페이징)
     */
    SliceResponseDto<UserResponseDto.CompletedChallengeDto> getCompletedChallenges(
            Long userId,
            int page,
            int size
    );

    /**
     * 현재 로그인한 사용자가 스크랩한 인증글 목록 조회 (페이징)
     */
    SliceResponseDto<UserResponseDto.ScrappedVerificationDto> getScrappedVerifications(
            User currentUser,
            VerificationPostType type,
            int page,
            int size
    );
  
    /**
     * 사용자 기본 정보 수정
     */
    UpdateUserInfoResponseDto updateUserInfo(
            Long userId,
            UpdateUserInfoRequestDto requestDto
    );
}
