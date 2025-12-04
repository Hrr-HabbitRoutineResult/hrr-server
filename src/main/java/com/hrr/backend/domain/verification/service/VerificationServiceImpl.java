package com.hrr.backend.domain.verification.service;

import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.verification.dto.*;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.verification.converter.VerificationConverter;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import com.hrr.backend.domain.auth.service.JwtService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VerificationServiceImpl implements VerificationService {

    private final UserChallengeRepository userChallengeRepository;
    private final VerificationRepository verificationRepository;
    private final RoundRecordRepository roundRecordRepository;

    /* URL 검증용 정규식 */
    private static final String URL_REGEX =
            "^(https?://)[\\w.-]+(?:\\.[\\w\\.-]+)+[/#?]?.*$";
    private final JwtService jwtService;

    private void validateTextUrlFormat(String textUrl) {
        if (textUrl == null || textUrl.isBlank()) return; // 비어있으면 허용
        if (!textUrl.matches(URL_REGEX)) {
            throw new GlobalException(ErrorCode.VERIFICATION_URL_INVALID);
        }
    }

    /** 텍스트 인증 */
    @Override
    public VerificationResponseDto createTextVerification(
            Long challengeId,
            Long roundId,
            Long userId,
            VerificationRequestDto request
    ) {

        UserChallenge uc = userChallengeRepository
                .findByUser_IdAndChallenge_Id(userId, challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_USER_CHALLENGE_NOT_FOUND));

        validateTextUrlFormat(request.getTextUrl());

        RoundRecord roundRecord = roundRecordRepository.findByUserChallengeAndRoundId(uc, roundId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

        Verification verification = Verification.createTextVerification(
                uc,
                roundRecord,
                request.getTitle(),
                request.getContent(),
                request.getTextUrl(),
                request.getIsQuestion(),
                roundId
        );

        Verification saved = verificationRepository.save(verification);
        return VerificationConverter.toResponse(saved);
    }

    /** 사진 인증 */
    @Override
    public VerificationResponseDto createPhotoVerification(
            Long challengeId,
            Long roundId,
            Long userId,
            MultipartFile file,
            String title,
            Boolean isQuestion
    ) {

        UserChallenge uc = userChallengeRepository
                .findByUser_IdAndChallenge_Id(userId, challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_USER_CHALLENGE_NOT_FOUND));

        // 임시 URL 생성
        String fakeUrl = "https://dummy.image/" + UUID.randomUUID();

        RoundRecord roundRecord = roundRecordRepository.findByUserChallengeAndRoundId(uc, roundId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

        Verification verification = Verification.createPhotoVerification(
                uc,
                roundRecord,
                title,
                fakeUrl,
                isQuestion,
                roundId
        );

        Verification saved = verificationRepository.save(verification);
        return VerificationConverter.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationMyResponseDto.MyPostList getMyVerifications(
            String accessToken,
            Long challengeId,
            Long roundId,
            Pageable pageable
    ) {
        // 토큰 분리 (Controller에서 null 체크했음)
        String token = accessToken.startsWith("Bearer ")
                ? accessToken.substring(7)
                : accessToken;

        Long userId = jwtService.extractUserId(token);

        /** 필터 조합 검증 */
        if (challengeId == null && roundId != null) {
            throw new GlobalException(ErrorCode.VERIFICATION_FILTER_INVALID);
        }

        /** roundId 검증 */
        if (roundId != null && roundId < 1) {
            throw new GlobalException(ErrorCode.VERIFICATION_ROUND_INVALID);
        }

        // Round 검증 필요 시
         /*if (!roundRepository.existsById(roundId)) {
             throw new GlobalException(ErrorCode.VERIFICATION_ROUND_INVALID);
         }*/

        Page<Verification> page;

        if (challengeId != null && roundId != null) {
            page = verificationRepository
                    .findByUserChallenge_User_IdAndUserChallenge_Challenge_IdAndRoundId(
                            userId,
                            challengeId,
                            roundId,
                            pageable
                    );
        } else {
            page = verificationRepository
                    .findByUserChallenge_User_Id(userId, pageable);
        }

        return VerificationConverter.toMyPostList(page);
    }

    @Override
    @Transactional(readOnly = true)
    public VerificationDetailResponseDto getVerificationDetail(Long verificationId) {

        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        return VerificationConverter.toDetailResponse(verification);
    }
    @Override
    @Transactional(readOnly = true)
    public VerificationListResponseDto.ListResponse getVerificationsByChallengeAndRound(
            Long challengeId,
            Long roundId,
            Pageable pageable
    ) {

        /** 1) challengeId, roundId 필수값 검증 */
        if (challengeId == null) {
            throw new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND);
        }
        /*주석 처리한건 round가 존재하지 않을 때*/
//        if (roundId == null) {
//            throw new GlobalException(ErrorCode.VERIFICATION_ROUND_INVALID);
//        }

        /** 2) roundId 유효성 검증 (1 이상이어야 함) */
//        if (roundId < 1) {
//            throw new GlobalException(ErrorCode.VERIFICATION_ROUND_INVALID);
//        }

        /** 3) (선택) 라운드 존재 여부 검증 — Round Entity 구조가 없어서 주석 처리 */
        // if (!roundRepository.existsByChallengeIdAndRoundId(challengeId, roundId)) {
        //     throw new GlobalException(ErrorCode.VERIFICATION_ROUND_NOT_FOUND);
        // }

        /** 4) challengeId + roundId 기준으로 전체 인증글 조회 */
//        Page<Verification> page =
//                verificationRepository.findByUserChallenge_Challenge_IdAndRoundId(
//                        challengeId,
//                        roundId,
//                        pageable
//                );
        Page<Verification> page;

        if (roundId == null) {

            page = verificationRepository.findByUserChallenge_Challenge_Id(
                    challengeId,
                    pageable
            );

        } else {

            /** roundId가 들어온 경우만 필터링 */
            if (roundId < 1) {
                throw new GlobalException(ErrorCode.VERIFICATION_ROUND_INVALID);
            }

            page = verificationRepository.findByUserChallenge_Challenge_IdAndRoundId(
                    challengeId,
                    roundId,
                    pageable
            );
        }

        /** 5) DTO 변환 */
        return VerificationConverter.toChallengeRoundList(page);
    }


}
