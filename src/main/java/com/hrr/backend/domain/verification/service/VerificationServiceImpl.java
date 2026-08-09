package com.hrr.backend.domain.verification.service;
import com.hrr.backend.domain.point.event.VerificationPointTriggerEvent;
import org.springframework.context.ApplicationEventPublisher;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.global.util.ChallengeVerificationWindowUtil;
import com.hrr.backend.domain.user.repository.UserBlockRepository;
import com.hrr.backend.global.common.enums.ChallengeDays;

import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.comment.dto.CommentResponseDto;
import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.comment.repository.CommentRepository;
import com.hrr.backend.domain.comment.service.CommentService;
import com.hrr.backend.domain.notification.event.QuestionVerificationCreatedEvent;
import com.hrr.backend.domain.verification.dto.VerificationUpdateRequestDto;
import org.springframework.context.ApplicationEventPublisher;
import com.hrr.backend.domain.point.service.PointService;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.user.repository.UserBlockRepository;
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
import com.hrr.backend.domain.verification.repository.VerificationLikeRepository;
import com.hrr.backend.domain.verification.repository.VerificationScrapRepository;
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
    private final UserBlockRepository userBlockRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PointService pointService;
    private final VerificationScrapRepository verificationScrapRepository;
    private final VerificationLikeRepository verificationLikeRepository;


    @Override
    public SliceResponseDto<VerificationResponseDto.FeedDto> getVerificationFeed(
            Long challengeId,
            Integer roundNumber,
            Long currentUserId,
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
                currentUserId,
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

        Round currentRound = challenge.getCurrentRound();

        // 라운드 미시작 시 0명 반환
        if (currentRound == null) {
            return verificationConverter.toStatDto(0, 0, null);
        }

		// 퇴출된 유저 제외하고 조회
        // 탈퇴 후 재활성화 가능 기간을 고려하여, 비활성(INACTIVE) 유저도 참여 상태가 유지되는 한 통계에 포함함
        Integer totalParticipantCount = roundRecordRepository.countByRoundIdAndJoinStatus(
                currentRound.getId(),
                ChallengeJoinStatus.JOINED
        );

        // 인증 요일/시간대가 아니면 null 반환
        LocalDateTime targetDateTime = determineTargetDateTime(challenge);

        if (targetDateTime == null) {
            // 인증 시간이 아니면 분자(certifiedCount)는 무조건 0
            return verificationConverter.toStatDto(0, totalParticipantCount, null);
        }

        LocalDate targetDate = targetDateTime.toLocalDate();

        // 인증한 사람 중 퇴출한 유저 제외
        Long certifiedCount = verificationRepository.countDistinctCertifiers(
                currentRound.getId(),
                VerificationStatus.COMPLETED,
                ChallengeJoinStatus.JOINED,
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
// 중복 인증 체크 로직 추가
        Challenge challenge = round.getChallenge();
        validateVerificationWindow(challenge, userChallenge.getId());



        Verification verification = Verification.createTextVerification(
                userChallenge,
                roundRecord,
                request.getTitle(),
                request.getContent(),
                request.getTextUrl(),
                request.getTextImages(), // DTO의 List<String> 전달
                request.getIsQuestion() != null ? request.getIsQuestion() : false,
                roundId
        );

        //동시성 문제 해결을 위해 flush()와 예외 처리 추가
        try {
            Verification savedVerification = verificationRepository.save(verification);
            verificationRepository.flush(); // DB 제약 조건 위반을 즉시 확인
            roundRecord.increaseVerificationCount();
            publishQuestionVerificationCreatedEventIfNeeded(savedVerification, userId);
            //포인트 기능
            eventPublisher.publishEvent(new VerificationPointTriggerEvent(savedVerification.getId()));
            return verificationConverter.toResponseDto(savedVerification);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 중복된 인증 생성이 시도될 경우 에러 반환
            throw new GlobalException(ErrorCode.VERIFICATION_ALREADY_EXISTS);
        }
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

        //중복 인증 체크 로직 추가
        Challenge challenge = round.getChallenge();
        validateVerificationWindow(challenge, userChallenge.getId());

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

        //동시성 문제 해결을 위해 flush()와 예외 처리 추가
        try {
            Verification saved = verificationRepository.save(verification);
            verificationRepository.flush(); // DB 제약 조건 위반을 즉시 확인
            roundRecord.increaseVerificationCount();
            publishQuestionVerificationCreatedEventIfNeeded(saved, userId);
            //포인트 기능
            eventPublisher.publishEvent(new VerificationPointTriggerEvent(saved.getId()));
            return verificationConverter.toResponseDto(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 중복된 인증 생성이 시도될 경우 에러 반환
            throw new GlobalException(ErrorCode.VERIFICATION_ALREADY_EXISTS);
        }
    }

    /**
     * 인증 중복 방지 로직
     * - 해당 챌린지의 인증 요일, 인증 시간대당 한 번만 인증 가능
     * - 인증 시간 동안에는 삭제 후 재인증 가능
     */
    private void validateVerificationWindow(Challenge challenge, Long userChallengeId) {
        LocalDateTime now = LocalDateTime.now();

        // 1) 오늘이 인증 요일인지 확인
        if (!isVerificationDay(challenge, now.toLocalDate())) {
            throw new GlobalException(ErrorCode.VERIFICATION_DAY_INVALID);
        }

        // 2) 현재 시간이 인증 시간대 내인지 확인
        if (!ChallengeVerificationWindowUtil.isWithinVerificationTime(challenge, now.toLocalTime())) {
            throw new GlobalException(ErrorCode.VERIFICATION_TIME_INVALID);
        }

        // 3) 현재 인증 윈도우의 기준 날짜 계산
        LocalDate currentWindowDate = ChallengeVerificationWindowUtil.getWindowAnchorDate(challenge, now);
        // 4) 해당 윈도우 날짜의 시작/종료 시간 계산
        LocalDateTime windowStart = LocalDateTime.of(currentWindowDate, challenge.getVerifyStartTime());
        LocalDateTime windowEnd = LocalDateTime.of(currentWindowDate, challenge.getVerifyEndTime());

        // 5) 해당 시간대에 이미 완료된 인증이 있는지 확인
        boolean alreadyVerified = verificationRepository.existsTodayVerification(
                userChallengeId,
                VerificationStatus.COMPLETED,
                windowStart,
                windowEnd
        );

        if (alreadyVerified) {
            throw new GlobalException(ErrorCode.VERIFICATION_ALREADY_EXISTS);
        }
    }

    private void publishQuestionVerificationCreatedEventIfNeeded(Verification verification, Long actorId) {
        if (Boolean.TRUE.equals(verification.getIsQuestion())) {
            eventPublisher.publishEvent(new QuestionVerificationCreatedEvent(verification.getId(), actorId));
        }
    }


    private VerificationPostType mapToPostType(VerificationType verificationType) {
        if (verificationType == VerificationType.TEXT) {
            return VerificationPostType.TEXT;
        } else {
            return VerificationPostType.CAMERA;
        }
    }

    private LocalDateTime determineTargetDateTime(Challenge challenge) {
        LocalDateTime now = LocalDateTime.now();

        // 오늘이 인증 요일인지 체크
        if (!isVerificationDay(challenge, now.toLocalDate())) {
            return null;
        }

        // 현재 시간이 인증 시간대 내인지 체크
        if (!ChallengeVerificationWindowUtil.isWithinVerificationTime(challenge, now.toLocalTime())) {
            return null;
        }

        return now;
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


        RoundRecord roundRecord = verification.getRoundRecord();
        User author = roundRecord.getUserChallenge().getUser();
        Challenge challenge = roundRecord.getRound().getChallenge();

        validateVerificationPostAccess(verification, author, currentUserId);

        // 3. 권한 체크용 변수 설정
        boolean isMine = currentUserId != null && author.getId().equals(currentUserId);

        // 4. 비공개 챌린지 접근 제한 로직
        if (Boolean.FALSE.equals(challenge.getIsPublic())) {
            // 내가 참여 중(JOINED)인지 확인
            boolean isJoined = currentUserId != null &&
                    userChallengeRepository.findByUserIdAndChallengeId(currentUserId, challenge.getId())
                            .map(uc -> uc.getStatus() == ChallengeJoinStatus.JOINED)
                            .orElse(false);

            // 작성자 본인도 아니고, 참여자도 아니라면 접근 거부
            if (!isMine && !isJoined) {
                throw new GlobalException(ErrorCode.VERIFICATION_ACCESS_DENIED);
            }
        }

        boolean isResolved = Boolean.TRUE.equals(verification.getIsResolved());
        boolean canEdit = isMine && !isResolved;
        boolean canDelete = isMine && !isResolved;

        boolean canSelectComment =
                isMine
                        && Boolean.TRUE.equals(verification.getIsQuestion())
                        && !isResolved;

		boolean canWriteComment = false;
		if (currentUserId != null) {
			Long challengeId = verification.getRoundRecord().getRound().getChallenge().getId();
			boolean isCurrentRound = verification.getRoundRecord().getRound().equals(
				verification.getRoundRecord().getRound().getChallenge().getCurrentRound());	// 현재 라운드인지 체크
			boolean isParticipating = userChallengeRepository.findByUserIdAndChallengeId(currentUserId, challengeId)
				.map(uc -> uc.getStatus() == ChallengeJoinStatus.JOINED)// 해당 챌린지에 참여 중인지 체크
				.orElse(false);
			canWriteComment = isParticipating && isCurrentRound;
		}

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
                canWriteComment,
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

        // 인증 요일 + 인증 시간대 안에서만 수정 가능
        validateVerificationEditDeleteWindow(verification);

        // 엔티티 업데이트
        verification.update(
                requestDto.getTitle(),
                requestDto.getContent(),
                requestDto.getTextUrl(),
                requestDto.getTextImages(),
                requestDto.getIsQuestion()
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

        boolean canWriteComment = false;
        if (currentUserId != null) {
			Long challengeId = verification.getRoundRecord().getRound().getChallenge().getId();
			boolean isCurrentRound = verification.getRoundRecord().getRound().equals(
						verification.getRoundRecord().getRound().getChallenge().getCurrentRound());	// 현재 라운드인지 체크
			boolean isParticipating = userChallengeRepository.findByUserIdAndChallengeId(currentUserId, challengeId)
						.map(uc -> uc.getStatus() == ChallengeJoinStatus.JOINED)// 해당 챌린지에 참여 중인지 체크
						.orElse(false);
			canWriteComment = isParticipating && isCurrentRound;
		}

        return verificationConverter.toDetailDto(
                verification,
                comments,
                isMine,
                canEdit,
                canDelete,
                canSelectComment,
                canWriteComment,
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

        // 인증 요일 + 인증 시간대 안에서만 삭제 가능
        validateVerificationEditDeleteWindow(verification);

        // point_history가 verification을 FK로 참조하므로, verification을 지우기 전에 먼저 회수 처리해야 함
        pointService.revokePointsForVerification(verification);

        verificationRepository.delete(verification);
    }

    @Override
    @Transactional
    public VerificationResponseDto.ScrapResponseDto scrapVerification(Long verificationId, User currentUser) {
        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        validateVerificationRoundParticipation(verification, currentUser);
        validateVerificationPostAccess(verification, currentUser.getId());

        verificationScrapRepository.insertIgnore(currentUser.getId(), verificationId);

        return verificationConverter.toScrapResponseDto(verification);
    }

    @Override
    @Transactional
    public VerificationResponseDto.ScrapResponseDto unscrapVerification(Long verificationId, User currentUser) {
        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        validateVerificationRoundParticipation(verification, currentUser);
        validateVerificationPostAccess(verification, currentUser.getId());

        verificationScrapRepository.deleteByUserIdAndVerificationId(currentUser.getId(), verificationId);

        return verificationConverter.toScrapResponseDto(verification, false);
    }

    @Override
    @Transactional
    public VerificationResponseDto.LikeResponseDto likeVerification(Long verificationId, User currentUser) {
        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        validateVerificationRoundParticipation(verification, currentUser);
        validateVerificationPostAccess(verification, currentUser.getId());

        verificationLikeRepository.insertIgnore(currentUser.getId(), verificationId);

        return verificationConverter.toLikeResponseDto(verification);
    }

    @Override
    @Transactional
    public VerificationResponseDto.LikeResponseDto unlikeVerification(Long verificationId, User currentUser) {
        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        validateVerificationRoundParticipation(verification, currentUser);
        validateVerificationPostAccess(verification, currentUser.getId());

        verificationLikeRepository.deleteByUserIdAndVerificationId(currentUser.getId(), verificationId);

        return verificationConverter.toLikeResponseDto(verification, false);
    }

    private void validateVerificationRoundParticipation(Verification verification, User currentUser) {
        Round postRound = verification.getRoundRecord().getRound();
        Long challengeId = postRound.getChallenge().getId();

        UserChallenge userChallenge = userChallengeRepository
                .findByUserIdAndChallengeId(currentUser.getId(), challengeId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_ACCESS_DENIED));

        roundRecordRepository.findByUserChallengeAndRoundId(userChallenge, postRound.getId())
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_ACCESS_DENIED));
    }

    /**
     * 게시글 작성자를 조회한 뒤 인증 게시글 접근 권한을 검증합니다.
     */
    private void validateVerificationPostAccess(Verification verification, Long currentUserId) {
        User author = verification.getRoundRecord()
                .getUserChallenge()
                .getUser();

        validateVerificationPostAccess(verification, author, currentUserId);
    }

    private void validateVerificationPostAccess(Verification verification, User author, Long currentUserId) {
        // 차단된 게시글 접근 시 예외 발생
        if (verification.getStatus() == VerificationStatus.BLOCKED) {
            throw new GlobalException(ErrorCode.ACCESS_DENIED_REPORTED_POST);
        }

        // 현재 사용자가 로그인한 경우 차단 관계 확인
        if (author.isNotActive()) {
            throw new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND);
        }

        if (currentUserId == null) {
            return;
        }

        // 내가 작성자를 차단한 경우
        boolean iBlockedAuthor = userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, author.getId());
        if (iBlockedAuthor) {
            throw new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND);
        }

        // 작성자가 나를 차단한 경우
        boolean authorBlockedMe = userBlockRepository.existsByBlockerIdAndBlockedId(author.getId(), currentUserId);
        if (authorBlockedMe) {
            throw new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND);
        }
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

    // 수정/삭제 시간 제한 로직 (최소 범위)
    private void validateVerificationEditDeleteWindow(Verification verification) {
        Challenge challenge = verification.getRoundRecord()
                .getRound()
                .getChallenge();

        LocalDateTime now = LocalDateTime.now();

        // 1) 지금이 "인증 시간대"가 아니면 수정/삭제 불가
        if (!ChallengeVerificationWindowUtil.isWithinVerificationTime(challenge, now.toLocalTime())) {
            throw new GlobalException(ErrorCode.VERIFICATION_EDIT_DELETE_NOT_ALLOWED);
        }

        // 2) 지금 인증 윈도우가 어느 "인증 요일(기준 날짜)"에 속하는지 계산
        LocalDate currentWindowDate = ChallengeVerificationWindowUtil.getWindowAnchorDate(challenge, now);

        // 3) 그 날짜가 챌린지의 인증 요일에 포함되지 않으면 불가
        if (!isVerificationDay(challenge, currentWindowDate)) {
            throw new GlobalException(ErrorCode.VERIFICATION_EDIT_DELETE_NOT_ALLOWED);
        }

        // 4) 이 인증글이 "현재 인증 윈도우"에 속하는 글이 아니면 불가
        LocalDate postWindowDate = ChallengeVerificationWindowUtil.getWindowAnchorDate(challenge, verification.getCreatedAt());
        if (!postWindowDate.equals(currentWindowDate)) {
            throw new GlobalException(ErrorCode.VERIFICATION_EDIT_DELETE_NOT_ALLOWED);
        }
    }

    /**
     * 특정 날짜가 챌린지 인증 요일인지 확인
     */
    private boolean isVerificationDay(Challenge challenge, LocalDate date) {
        ChallengeDays targetDay = mapToChallengeDays(date.getDayOfWeek());
        List<ChallengeDayJoin> days = challenge.getChallengeDays();

        return days.stream()
                .anyMatch(join -> join.getDayOfWeek() == targetDay);
    }

    private ChallengeDays mapToChallengeDays(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> ChallengeDays.MONDAY;
            case TUESDAY -> ChallengeDays.TUESDAY;
            case WEDNESDAY -> ChallengeDays.WEDNESDAY;
            case THURSDAY -> ChallengeDays.THURSDAY;
            case FRIDAY -> ChallengeDays.FRIDAY;
            case SATURDAY -> ChallengeDays.SATURDAY;
            case SUNDAY -> ChallengeDays.SUNDAY;
        };
    }

    @Override
    public VerificationResponseDto.OtherUserHistoryResponse getOtherUserVerificationHistory(
            Long targetUserId,
            User currentUser,
            int page,
            int size
    ) {
        // 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        if (targetUser.isNotActive() || userBlockRepository.existsByBlockerAndBlocked(targetUser, currentUser)) {
            throw new GlobalException(ErrorCode.USER_NOT_FOUND);
        }

        // 차단 관계 시 빈 리스트 반환
        if (userBlockRepository.existsByBlockerAndBlocked(currentUser, targetUser)) {
            return VerificationResponseDto.OtherUserHistoryResponse.builder()
                    .isPublic(true)
                    .nickname(targetUser.getDisplayNickname()) // (알 수 없음) 표시
                    .verifications(new SliceResponseDto<>(new SliceImpl<>(Collections.emptyList(), PageRequest.of(page, size), false)))
                    .build();
        }

        // 비공개 계정 체크
        if (!targetUser.getIsPublic()) {
            log.info("User {} is private. Returning empty list.", targetUserId);
            return VerificationResponseDto.OtherUserHistoryResponse.builder()
                    .isPublic(false)
                    .nickname(targetUser.getNickname())
                    .verifications(new SliceResponseDto<>(Page.empty()))
                    .build();
        }

        // 공개 계정
        Pageable pageable = PageRequest.of(page, size);

        // 비공개 챌린지 인증 기록은 현재 사용자가 해당 챌린지의 JOINED 참여자인 경우에만 노출
        Slice<Verification> verificationSlice =
                verificationRepository.findFilteredVerificationHistory(
                        targetUser,
                        currentUser.getId(),
                        VerificationStatus.COMPLETED,
                        pageable);

        Slice<VerificationResponseDto.HistoryDto> dtoSlice =
                verificationSlice.map(verificationConverter::toHistoryDto);

        return VerificationResponseDto.OtherUserHistoryResponse.builder()
                .isPublic(true)
                .nickname(targetUser.getNickname())
                .verifications(new SliceResponseDto<>(dtoSlice))
                .build();
    }
}
