package com.hrr.backend.domain.verification.service;

import com.hrr.backend.domain.verification.dto.VerificationRequestDto;
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

    /**
     * 내 인증 현황(프로필) 조회 (신규 기능)
     */
    VerificationResponseDto.MyProfileDto getMyVerificationProfile(
            Long userId,
            Long challengeId,
            int page,
            int size
    );

    /**
     * 글(TEXT) 인증 생성
     * 반환 타입 변경: VerificationResponseDto -> VerificationResponseDto.CreateResponseDto
     */
    VerificationResponseDto.CreateResponseDto createTextVerification(
            Long challengeId,
            Long userId,
            VerificationRequestDto request
    );

    /**
     * 사진(PHOTO) 인증 생성
     * 반환 타입 변경: VerificationResponseDto -> VerificationResponseDto.CreateResponseDto
     */
    VerificationResponseDto.CreateResponseDto createPhotoVerification(
            Long challengeId,
            Long userId,
            String content,
            String s3Key,
            String title,
            Boolean isQuestion
    );
}