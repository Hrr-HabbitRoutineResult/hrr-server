package com.hrr.backend.domain.verification.service;

import com.hrr.backend.domain.verification.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface VerificationService {

    VerificationResponseDto createTextVerification(
            Long challengeId,
            Long roundId,
            Long userId,
            VerificationRequestDto request
    );

    VerificationResponseDto createPhotoVerification(
            Long challengeId,
            Long roundId,
            Long userId,
            MultipartFile file,
            String title,
            Boolean isQuestion
    );
/* 사용자 본인 인증글 목록 조회 */
    VerificationMyResponseDto.MyPostList getMyVerifications(
            String accessToken,
            Long challengeId,
            Long roundId,
            Pageable pageable
    );

    /*인증 글 상세 조회*/
    VerificationDetailResponseDto getVerificationDetail(Long verificationId);

    /* 챌린지 + 라운드별 인증글 목록 조회 */
    VerificationListResponseDto.ListResponse getVerificationsByChallengeAndRound(
            Long challengeId,
            Long roundId,
            Pageable pageable
    );

}

