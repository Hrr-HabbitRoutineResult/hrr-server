package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.user.dto.UserResponseDto;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface UserChallengeRepositoryCustom {

    /**
     * 사용자가 참가중인 챌린지 목록 조회 (페이징)
     * @param user 조회 대상 사용자
     * @param pageable 페이징 정보
     * @return 참가중인 챌린지 정보 Slice
     */
    Slice<UserResponseDto.OngoingChallengeDto> findOngoingChallengesByUser(User user, Pageable pageable);
}