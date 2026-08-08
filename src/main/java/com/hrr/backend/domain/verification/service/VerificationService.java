package com.hrr.backend.domain.verification.service;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.dto.VerificationRequestDto;
import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationDetailResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationUpdateRequestDto;
import com.hrr.backend.global.response.SliceResponseDto;

public interface VerificationService {

    /**
     * 인증 피드 목록 조회 (페이징)
     */
    SliceResponseDto<VerificationResponseDto.FeedDto> getVerificationFeed(
            Long challengeId,
            Integer roundNumber,
            Long currentUserId,
            int page,
            int size
    );

    /**
     * 인증 인원 통계 조회 (상단 헤더용)
     */
    VerificationResponseDto.StatDto getVerificationStat(Long challengeId);

    /**
     * 챌린지 인증현황 마이 조회
     */
    VerificationResponseDto.MyProfileDto getMyVerificationProfile(
            User user,
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
  
    VerificationDetailResponseDto getVerificationDetail(Long verificationId, Long currentUserId, int page, int size);

    void adoptComment(Long verificationId, Long commentId, Long currentUserId);

    VerificationDetailResponseDto updateVerification(Long verificationId, Long currentUserId, VerificationUpdateRequestDto requestDto);

    void deleteVerification(Long verificationId, Long currentUserId);

    VerificationResponseDto.ScrapResponseDto scrapVerification(Long verificationId, User currentUser);

    VerificationResponseDto.ScrapResponseDto unscrapVerification(Long verificationId, User currentUser);
  
    /**
     * 사용자 인증 기록 조회
     */
    SliceResponseDto<VerificationResponseDto.HistoryDto> getVerificationHistory(
            Long userId,
            int page,
            int size
    );

    /**
     * 다른 사용자 전체 챌린지 인증 기록 조회
     */
    VerificationResponseDto.OtherUserHistoryResponse getOtherUserVerificationHistory(
            Long targetUserId,
            User me,
            int page,
            int size
    );
}
