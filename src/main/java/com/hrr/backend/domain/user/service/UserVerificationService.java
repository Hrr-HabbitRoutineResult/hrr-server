package com.hrr.backend.domain.user.service;

import com.hrr.backend.domain.user.dto.UserVerificationResponseDto;
import com.hrr.backend.global.response.SliceResponseDto;

public interface UserVerificationService {

    // 내 전체 인증 기록 조회
    SliceResponseDto<UserVerificationResponseDto.VerificationItemDto> getVerificationHistory(
            Long userId,
            int page,
            int size
    );
}