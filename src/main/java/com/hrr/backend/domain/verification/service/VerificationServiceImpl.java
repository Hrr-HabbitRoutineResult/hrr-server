package com.hrr.backend.domain.verification.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.comment.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
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
import com.hrr.backend.domain.verification.dto.VerificationDetailResponseDto;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationPostType;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.response.SliceResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificationServiceImpl implements VerificationService {

    private final VerificationRepository verificationRepository;
    private final ChallengeRepository challengeRepository;
    private final RoundRepository roundRepository;
    private final RoundRecordRepository roundRecordRepository;
    private final UserRepository userRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final VerificationConverter verificationConverter;
    private final CommentService commentService;


    @Override
    public SliceResponseDto<VerificationResponseDto.FeedDto> getVerificationFeed(
            Long challengeId,
            Integer roundNumber,
            int page,
            int size
    ) {
        // 1. 챌린지 조회
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

        // 2. 라운드 조회 (챌린지 ID + 라운드 번호)
        Round round = roundRepository.findByChallengeIdAndRoundNumber(challengeId, roundNumber)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROUND_NOT_FOUND));

        // 3. 타입 변환
        VerificationPostType targetType = mapToPostType(challenge.getVerificationType());

        // 4. DB 조회
        Pageable pageable = PageRequest.of(page, size);
        Page<Verification> verificationPage = verificationRepository.findVerificationFeed(
                round.getId(),
                targetType,
                VerificationStatus.COMPLETED,
                pageable
        );

        Slice<VerificationResponseDto.FeedDto> dtoSlice = verificationPage.map(verificationConverter::toFeedDto);
        return new SliceResponseDto<>(dtoSlice);
    }

    @Override
    public VerificationResponseDto.StatDto getVerificationStat(Long challengeId) {
        // 1. 챌린지 조회
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

        // 2. 상태 검증 (ONGOING, RECRUITING 아니면 에러)
        ChallengeStatus status = challenge.getStatus();
        if (status != ChallengeStatus.ONGOING && status != ChallengeStatus.RECRUITING) {
            throw new GlobalException(ErrorCode.CHALLENGE_NOT_IN_PROGRESS);
        }

        Integer totalParticipantCount = challenge.getCurrentParticipants();
        Round currentRound = challenge.getCurrentRound();
        
        // 라운드 미시작 시 0명 반환
        if (currentRound == null) {
            return verificationConverter.toStatDto(0, totalParticipantCount, null);
        }

        Long currentRoundId = currentRound.getId();
        LocalDateTime targetDateTime = determineTargetDateTime(challenge, currentRoundId);

        if (targetDateTime == null) {
            return verificationConverter.toStatDto(0, totalParticipantCount, null);
        }

        LocalDate targetDate = targetDateTime.toLocalDate();
        Long certifiedCount = verificationRepository.countDistinctCertifiers(
                currentRoundId,
                VerificationStatus.COMPLETED,
                targetDate.atStartOfDay(),
                targetDate.atTime(LocalTime.MAX)
        );

        return verificationConverter.toStatDto(certifiedCount.intValue(), totalParticipantCount, targetDateTime);
    }

    @Override
    public VerificationResponseDto.MyProfileDto getMyVerificationProfile(
            Long userId,
            Long challengeId,
            int page,
            int size
    ) {
        // 1. 유저 챌린지 조회
        UserChallenge userChallenge = userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_ALREADY_JOINED)); // 적절한 에러코드(NOT_JOINED) 필요 시 수정

        Long userChallengeId = userChallenge.getId();

        // 2. 통계 계산
        Long roundSequence = roundRecordRepository.countByUserChallengeId(userChallengeId);
        Long totalVerification = roundRecordRepository.sumVerificationCountByUserChallengeId(userChallengeId);
        Integer warningCount = userChallenge.getKickWarnings();

        // 3. 내 인증글 조회
        Pageable pageable = PageRequest.of(page, size);
        Page<Verification> verificationPage = verificationRepository.findMyVerifications(
                userChallengeId,
                VerificationStatus.COMPLETED,
                pageable
        );

        Slice<VerificationResponseDto.FeedDto> dtoSlice = verificationPage.map(verificationConverter::toFeedDto);

        return VerificationResponseDto.MyProfileDto.builder()
                .nickname(userChallenge.getUser().getNickname())
                .totalVerificationCount(totalVerification)
                .warningCount(warningCount)
                .currentRoundSequence(roundSequence)
                .verifications(new SliceResponseDto<>(dtoSlice))
                .build();
    }

    @Override
    @Transactional
    public VerificationResponseDto.CreateResponseDto createTextVerification(
            Long challengeId,
            Long userId,
            VerificationRequestDto request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 현재 라운드 조회
        Round round = roundRepository.findCurrentRoundByChallengeId(challengeId, LocalDate.now())
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_ROUND_INVALID)); // 에러코드 확인 필요
        Long roundId = round.getId();

        UserChallenge userChallenge = userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_CHALLENGE_NOT_FOUND)); // 에러코드 확인 필요

        RoundRecord roundRecord = roundRecordRepository.findByUserChallengeAndRoundId(userChallenge, roundId)
                .orElseThrow(() -> new GlobalException(ErrorCode.ROUND_RECORD_NOT_FOUND)); // 에러코드 확인 필요

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
    public VerificationResponseDto.CreateResponseDto createPhotoVerification(
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

        UserChallenge userChallenge = userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_CHALLENGE_NOT_FOUND));

        RoundRecord roundRecord = roundRecordRepository.findByUserChallengeAndRoundId(userChallenge, roundId)
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

    private VerificationPostType mapToPostType(VerificationType verificationType) {
        if (verificationType == VerificationType.TEXT) {
            return VerificationPostType.TEXT;
        } else {
            return VerificationPostType.CAMERA;
        }
    }

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

    @Override
    @Transactional(readOnly = true)
    public VerificationDetailResponseDto getVerificationDetail(Long verificationId, Long currentUserId, int page, int size) {

        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        RoundRecord roundRecord = verification.getRoundRecord();
        UserChallenge userChallenge = roundRecord.getUserChallenge();
        User author = userChallenge.getUser();

        boolean isMine = currentUserId != null && author.getId().equals(currentUserId);

        boolean canEdit = isMine;
        boolean canDelete = isMine;
        boolean canSelectComment =
                isMine
                        && Boolean.TRUE.equals(verification.getIsQuestion())
                        && !Boolean.TRUE.equals(verification.getIsResolved());

        Pageable pageable = PageRequest.of(page, size);
        CommentListResponseDto comments = commentService.getComments(verificationId, pageable);

        return verificationConverter.toDetailDto(
                verification,
                comments,
                isMine,
                canEdit,
                canDelete,
                canSelectComment
        );
    }

}