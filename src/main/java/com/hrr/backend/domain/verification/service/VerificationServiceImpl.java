package com.hrr.backend.domain.verification.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.verification.converter.VerificationConverter;
import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.common.enums.VerificationType;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificationServiceImpl implements VerificationService {

    private final VerificationRepository verificationRepository;
    private final ChallengeRepository challengeRepository;
    private final RoundRepository roundRepository;
    private final VerificationConverter verificationConverter;

    @Override
    public SliceResponseDto<VerificationResponseDto.FeedDto> getVerificationFeed(
            Long challengeId,
            Integer roundNumber,
            int page,
            int size
    ) {
        // 챌린지 조회 (검증 및 타입 확인용)
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

        // 챌린지 ID와 라운드 번호로 라운드 조회
        Round round = roundRepository.findByChallengeIdAndRoundNumber(challengeId, roundNumber)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROUND_NOT_FOUND));

        // Challenge.VerificationType -> VerificationPostType 변환
        VerificationPostType targetType = mapToPostType(challenge.getVerificationType());

        Pageable pageable = PageRequest.of(page, size);

        // Repository 조회
        Page<Verification> verificationPage = verificationRepository.findVerificationFeed(
                round.getId(), // 조회된 라운드의 PK 사용
                targetType,
                VerificationStatus.COMPLETED,
                pageable
        );

        Slice<VerificationResponseDto.FeedDto> dtoSlice = verificationPage.map(verificationConverter::toFeedDto);
        return new SliceResponseDto<>(dtoSlice);
    }


    @Override
    public VerificationResponseDto.StatDto getVerificationStat(Long challengeId) {
        // 챌린지 조회
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

        // 챌린지 상태 검증 (ONGOING, RECRUITING 아니면 에러)
        ChallengeStatus status = challenge.getStatus();
        if (status != ChallengeStatus.ONGOING && status != ChallengeStatus.RECRUITING) {
            throw new GlobalException(ErrorCode.CHALLENGE_NOT_IN_PROGRESS);
        }

        // 현재 참가자 수 (위 검증을 통과했으므로 무조건 존재)
        Integer totalParticipantCount = challenge.getCurrentParticipants();

        // 현재 진행 중인 라운드 조회
        Round currentRound = challenge.getCurrentRound();
        if (currentRound == null) {
            return verificationConverter.toStatDto(0, totalParticipantCount, null);
        }

        Long currentRoundId = currentRound.getId();

        // 기준 시간 결정
        LocalDateTime targetDateTime = determineTargetDateTime(challenge, currentRoundId);

        if (targetDateTime == null) {
            return verificationConverter.toStatDto(0, totalParticipantCount, null);
        }

        // 인증 인원 집계
        LocalDate targetDate = targetDateTime.toLocalDate();
        Integer certifiedCount = verificationRepository.countDistinctCertifiers(
                currentRoundId,
                VerificationStatus.COMPLETED,
                targetDate.atStartOfDay(),
                targetDate.atTime(LocalTime.MAX)
        );

        return verificationConverter.toStatDto(certifiedCount, totalParticipantCount, targetDateTime);
    }

    // 타입 변환 헬퍼 메서드
    private VerificationPostType mapToPostType(VerificationType verificationType) {
        // Challenge Entity의 VerificationType(PHOTO, TEXT)을
        // Verification Entity의 VerificationPostType(CAMERA, TEXT)으로 매핑
        if (verificationType == VerificationType.TEXT) {
            return VerificationPostType.TEXT;
        } else {
            // 나머지는 CAMERA로 취급
            return VerificationPostType.CAMERA;
        }
    }

    // 기준 시간 결정 메서드
    private LocalDateTime determineTargetDateTime(Challenge challenge, Long roundId) {
        LocalTime now = LocalTime.now();
        LocalTime start = challenge.getVerifyStartTime();
        LocalTime end = challenge.getVerifyEndTime();

        boolean isVerificationTime;
        if (start.isBefore(end)) {
            isVerificationTime = !now.isBefore(start) && !now.isAfter(end);
        } else {
            isVerificationTime = !now.isBefore(start) || !now.isAfter(end);
        }

        if (isVerificationTime) {
            return LocalDateTime.now();
        } else {
            return verificationRepository.findLatestVerificationTime(
                    roundId,
                    VerificationStatus.COMPLETED
            );
        }
    }

}