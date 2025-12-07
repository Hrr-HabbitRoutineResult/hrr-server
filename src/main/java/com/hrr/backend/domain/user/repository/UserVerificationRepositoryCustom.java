package com.hrr.backend.domain.user.repository;

import com.hrr.backend.domain.user.dto.UserVerificationResponseDto;
import com.hrr.backend.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface UserVerificationRepositoryCustom {

    /**
     * 사용자의 전체 인증 기록 조회 (페이징)
     * @param user 조회 대상 사용자
     * @param pageable 페이징 정보
     * @return 인증 기록 Slice
     */
    Slice<UserVerificationResponseDto.VerificationItemDto> findVerificationHistoryByUser(User user, Pageable pageable);
}