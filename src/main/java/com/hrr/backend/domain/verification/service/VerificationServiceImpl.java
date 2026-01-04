package com.hrr.backend.domain.verification.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.comment.dto.CommentResponseDto;
import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.comment.repository.CommentRepository;
import com.hrr.backend.domain.comment.service.CommentService;
import com.hrr.backend.domain.verification.dto.VerificationUpdateRequestDto;
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
    private final CommentRepository commentRepository;


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
            User user,
            Long challengeId,
            int page,
            int size
    ) {
        // 유저 챌린지 조회
        UserChallenge userChallenge = userChallengeRepository.findByUserIdAndChallengeId(user.getId(), challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_CHALLENGE_NOT_FOUND));

        Long userChallengeId = userChallenge.getId();

        // 통계 데이터 조회
        Long roundSequence = roundRecordRepository.countByUserChallengeId(userChallengeId);
        Long totalVerification = roundRecordRepository.sumVerificationCountByUserChallengeId(userChallengeId);

        // 내 인증글 목록 조회
        Pageable pageable = PageRequest.of(page, size);
        Page<Verification> verificationPage = verificationRepository.findMyVerifications(
                userChallengeId,
                VerificationStatus.COMPLETED,
                pageable
        );

        // 목록 변환
        Slice<VerificationResponseDto.FeedDto> dtoSlice = verificationPage.map(verificationConverter::toFeedDto);
        SliceResponseDto<VerificationResponseDto.FeedDto> sliceResponse = new SliceResponseDto<>(dtoSlice);

        // 최종 응답 변환
        return verificationConverter.toMyProfileDto(
                userChallenge,
                totalVerification,
                roundSequence,
                sliceResponse
        );
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
    public VerificationDetailResponseDto getVerificationDetail(
            Long verificationId,
            Long currentUserId,
            int page,
            int size
    ) {
        // 인증글 조회
        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));


        // 차단된 게시글 접근 시 예외 발생
        if (verification.getStatus() == VerificationStatus.BLOCKED) {
            throw new GlobalException(ErrorCode.ACCESS_DENIED_REPORTED_POST);
        }

        RoundRecord roundRecord = verification.getRoundRecord();
        UserChallenge userChallenge = roundRecord.getUserChallenge();
        User author = userChallenge.getUser();

        boolean isMine = currentUserId != null && author.getId().equals(currentUserId);
        boolean isResolved = Boolean.TRUE.equals(verification.getIsResolved());
        boolean canEdit = isMine && !isResolved;
        boolean canDelete = isMine && !isResolved;

        boolean canSelectComment =
                isMine
                        && Boolean.TRUE.equals(verification.getIsQuestion())
                        && !isResolved;
        Pageable pageable = PageRequest.of(page, size);
        CommentListResponseDto comments = commentService.getComments(verificationId, currentUserId, pageable);

        Long adoptedCommentId = comments.getComments().stream()
                .filter(CommentResponseDto::isAdopted)
                .map(CommentResponseDto::getCommentId)
                .findFirst()
                .orElse(null);

        return verificationConverter.toDetailDto(
                verification,
                comments,
                isMine,
                canEdit,
                canDelete,
                canSelectComment,
                adoptedCommentId
        );
    }

    @Transactional
    @Override
    public void adoptComment(Long verificationId, Long commentId, Long currentUserId) {

        // 인증글 조회
        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        // 작성자(질문 올린 유저) 확인
        RoundRecord roundRecord = verification.getRoundRecord();
        UserChallenge userChallenge = roundRecord.getUserChallenge();
        User author = userChallenge.getUser();
        Long authorId = author.getId();

        if (!author.getId().equals(currentUserId)) {
            throw new GlobalException(ErrorCode.VERIFICATION_ACCESS_DENIED);
        }

        // 질문 인증글인지 검증
        if (!Boolean.TRUE.equals(verification.getIsQuestion())) {
            throw new GlobalException(ErrorCode.VERIFICATION_NOT_QUESTION);
        }

        // 이미 해결된 인증글인지 검증 (한 번 채택하면 다시 못 바꾸게 막기)
        if (Boolean.TRUE.equals(verification.getIsResolved())) {
            throw new GlobalException(ErrorCode.VERIFICATION_ALREADY_RESOLVED);
        }

        // 댓글 조회
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        // 해당 인증글의 댓글인지 검증
        if (!comment.getVerification().getId().equals(verificationId)) {
            throw new GlobalException(ErrorCode.COMMENT_INVALID);
        }

        // 도메인 메서드 호출로 상태 변경
        comment.adopt();        // 댓글 채택
        verification.resolve(); // 인증글 해결 상태로 변경
    }

    @Override
    @Transactional
    public VerificationDetailResponseDto updateVerification(Long verificationId, Long currentUserId, VerificationUpdateRequestDto requestDto) {

        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

		// 차단된 게시글 접근 시 예외 발생
		if (verification.getStatus() == VerificationStatus.BLOCKED) {
			throw new GlobalException(ErrorCode.ACCESS_DENIED_REPORTED_POST);
		}

        // 작성자 본인인지 권한 체크
        User author = verification.getRoundRecord()
                .getUserChallenge()
                .getUser();

        if (!author.getId().equals(currentUserId)) {
            throw new GlobalException(ErrorCode.VERIFICATION_ACCESS_DENIED);
        }

        // 엔티티 업데이트
        verification.update(
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getTextUrl(),
                requestDto.getPhotoUrl()
        );

        // 댓글 목록 + 상세 DTO 구성
        CommentListResponseDto comments = commentService.getComments(verificationId, currentUserId, PageRequest.of(0, 10));

        boolean isMine = currentUserId.equals(author.getId());
        boolean isResolved = Boolean.TRUE.equals(verification.getIsResolved());
        boolean canEdit = isMine && !isResolved;
        boolean canDelete = isMine && !isResolved;

        boolean canSelectComment = Boolean.TRUE.equals(verification.getIsQuestion())
                && !isResolved
                && isMine;
        Long adoptedCommentId = comments.getComments().stream()
                .filter(CommentResponseDto::isAdopted)
                .map(CommentResponseDto::getCommentId)
                .findFirst()
                .orElse(null);

        return verificationConverter.toDetailDto(
                verification,
                comments,
                isMine,
                canEdit,
                canDelete,
                canSelectComment,
                adoptedCommentId
        );
    }

    @Override
    @Transactional
    public void deleteVerification(Long verificationId, Long currentUserId) {

        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

		// 차단된 게시글 접근 시 예외 발생
		if (verification.getStatus() == VerificationStatus.BLOCKED) {
			throw new GlobalException(ErrorCode.ACCESS_DENIED_REPORTED_POST);
		}

    // 작성자 본인인지 권한 체크
        User author = verification.getRoundRecord()
                .getUserChallenge()
                .getUser();

        if (!author.getId().equals(currentUserId)) {
            throw new GlobalException(ErrorCode.VERIFICATION_ACCESS_DENIED);
        }
        verificationRepository.delete(verification);
    }

    /**
     * 사용자 전체 챌린지 인증 기록 조회
     */
    @Override
    public SliceResponseDto<VerificationResponseDto.HistoryDto> getVerificationHistory(
            Long userId,
            int page,
            int size
    ) {
        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // Pageable 객체 생성
        Pageable pageable = PageRequest.of(page, size);

        // Repository에서 인증 엔티티 조회
        Slice<Verification> verificationSlice =
                verificationRepository.findVerificationHistoryByUser(
                        user,
                        VerificationStatus.COMPLETED,
                        pageable);

        // 엔티티를 DTO로 변환 (빌더 패턴 사용)
        Slice<VerificationResponseDto.HistoryDto> dtoSlice =
                verificationSlice.map(verificationConverter::toHistoryDto);

        // SliceResponseDto로 변환하여 반환
        return new SliceResponseDto<>(dtoSlice);
    }

    @Override
    public VerificationResponseDto.OtherUserHistoryResponse getOtherUserVerificationHistory(
            Long userId,
            int page,
            int size
    ) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 2. 비공개 계정 체크
        if (!user.getIsPublic()) {
            log.info("User {} is private. Returning empty list.", userId);
            return VerificationResponseDto.OtherUserHistoryResponse.builder()
                    .isPublic(false)
                    .nickname(user.getNickname())
                    .verifications(new SliceResponseDto<>(Page.empty()))
                    .build();
        }

        // 3. 공개 계정
        Pageable pageable = PageRequest.of(page, size);

        Slice<Verification> verificationSlice =
                verificationRepository.findVerificationHistoryByUser(
                        user,
                        VerificationStatus.COMPLETED,
                        pageable);

        Slice<VerificationResponseDto.HistoryDto> dtoSlice =
                verificationSlice.map(verificationConverter::toHistoryDto);

        return VerificationResponseDto.OtherUserHistoryResponse.builder()
                .isPublic(true)
                .nickname(user.getNickname())
                .verifications(new SliceResponseDto<>(dtoSlice))
                .build();
    }
}