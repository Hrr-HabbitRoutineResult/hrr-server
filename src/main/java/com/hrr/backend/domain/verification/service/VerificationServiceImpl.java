package com.hrr.backend.domain.verification.service;

import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.verification.converter.VerificationConverter;
import com.hrr.backend.domain.verification.dto.VerificationRequestDto;
import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificationServiceImpl implements VerificationService {

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final RoundRepository roundRepository;
    private final RoundRecordRepository roundRecordRepository;
    private final VerificationConverter verificationConverter;

    @Override
    @Transactional
    public VerificationResponseDto createTextVerification(
            Long challengeId,
            Long userId,
            VerificationRequestDto request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        Round round = roundRepository.findCurrentRoundByChallengeId(challengeId, LocalDate.now())
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_ROUND_INVALID));

        Long roundId = round.getId();

        UserChallenge userChallenge = userChallengeRepository
                .findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_CHALLENGE_NOT_FOUND));

        RoundRecord roundRecord = roundRecordRepository
                .findByUserChallengeAndRoundId(userChallenge, roundId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROUND_RECORD_NOT_FOUND));

        Verification verification = Verification.createTextVerification(
                userChallenge,
                roundRecord,
                request.getTitle(),
                request.getContent(),
                request.getTextUrl(),
                request.getPhotoUrl(),
                request.getIsQuestion() != null ? request.getIsQuestion() : false,
                roundId
        );

        Verification savedVerification = verificationRepository.save(verification);

        roundRecord.increaseVerificationCount();

        return verificationConverter.toResponseDto(savedVerification);
    }

    @Override
    @Transactional
    public VerificationResponseDto createPhotoVerification(
            Long challengeId,
            Long userId,
            String content,
            String s3Key,
            String title,
            Boolean isQuestion
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        Round round = roundRepository.findCurrentRoundByChallengeId(challengeId, LocalDate.now())
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_ROUND_INVALID));

        Long roundId = round.getId();

        UserChallenge userChallenge = userChallengeRepository
                .findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_CHALLENGE_NOT_FOUND));

        RoundRecord roundRecord = roundRecordRepository
                .findByUserChallengeAndRoundId(userChallenge, roundId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROUND_RECORD_NOT_FOUND));

        String photoUrl = s3Key;

        Verification verification = Verification.createPhotoVerification(
                userChallenge,
                roundRecord,
                title,
                content,
                photoUrl,
                isQuestion != null ? isQuestion : false,
                roundId
        );

        Verification saved = verificationRepository.save(verification);

        roundRecord.increaseVerificationCount();

        return verificationConverter.toResponseDto(saved);
    }
}