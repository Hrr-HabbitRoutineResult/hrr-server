package com.hrr.backend.domain.verification.service;

import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.global.response.SliceResponseDto;

public interface VerificationService {

    /**
     * 인증 피드 목록 조회 (페이징)
     */
    SliceResponseDto<VerificationResponseDto.FeedDto> getVerificationFeed(
            Long challengeId,
            Integer roundNumber,
            int page,
            int size
    );

    /**
     * 인증 인원 통계 조회 (상단 헤더용)
     */
    VerificationResponseDto.StatDto getVerificationStat(Long challengeId);

}