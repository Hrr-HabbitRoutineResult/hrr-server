package com.hrr.backend.domain.verification.service;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.comment.service.CommentService;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.user.repository.UserBlockRepository;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.verification.converter.VerificationConverter;
import com.hrr.backend.domain.verification.dto.VerificationDetailResponseDto;
import com.hrr.backend.domain.verification.dto.VerificationResponseDto;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.VerificationLike;
import com.hrr.backend.domain.verification.entity.VerificationScrap;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.domain.verification.repository.VerificationLikeRepository;
import com.hrr.backend.domain.verification.repository.VerificationScrapRepository;
import com.hrr.backend.domain.point.service.PointService;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.global.common.enums.ChallengeDays;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.InOrder;
import org.mockito.Mockito;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VerificationServiceTest {

    @InjectMocks
    private VerificationServiceImpl verificationService;

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private VerificationConverter verificationConverter;

    @Mock
    private CommentService commentService;

    @Mock
    private UserChallengeRepository userChallengeRepository; // 추가됨

    @Mock
    private PointService pointService;

    @Mock
    private RoundRecordRepository roundRecordRepository;

    @Mock
    private VerificationScrapRepository verificationScrapRepository;

    @Mock
    private VerificationLikeRepository verificationLikeRepository;

    // --- Helper Methods ---

    private User createUser(Long id, UserStatus status) {
        return User.builder()
                .id(id)
                .userStatus(status)
                .nickname("User" + id)
                .build();
    }

    private Verification createVerification(Long id, User author, VerificationStatus status) {
        Challenge challenge = Challenge.builder().id(100L).title("Test Challenge").build();
        Round round = Round.builder().id(200L).challenge(challenge).build();

        UserChallenge userChallenge = UserChallenge.builder()
                .user(author) // 작성자 연결
                .challenge(challenge)
                .build();

        RoundRecord roundRecord = RoundRecord.builder()
                .round(round)
                .userChallenge(userChallenge)
                .build();

        return Verification.builder()
                .id(id)
                .roundRecord(roundRecord)
                .status(status)
                .isQuestion(false)
                .isResolved(false)
                .build();
    }

    private void givenRoundParticipation(Verification verification, User currentUser) {
        Round postRound = verification.getRoundRecord().getRound();
        Long challengeId = postRound.getChallenge().getId();
        UserChallenge userChallenge = UserChallenge.builder()
                .user(currentUser)
                .challenge(postRound.getChallenge())
                .build();
        RoundRecord userRoundRecord = RoundRecord.builder()
                .round(postRound)
                .userChallenge(userChallenge)
                .build();

        given(userChallengeRepository.findByUserIdAndChallengeId(currentUser.getId(), challengeId))
                .willReturn(Optional.of(userChallenge));
        given(roundRecordRepository.findByUserChallengeAndRoundId(userChallenge, postRound.getId()))
                .willReturn(Optional.of(userRoundRecord));
    }

    // --- Tests ---

    @Test
    @DisplayName("성공: 차단/탈퇴 문제 없는 정상 게시글 조회 (댓글 작성 가능)")
    void getVerificationDetail_Success_CanWriteComment() {
        // given
        Long verificationId = 1L;
        Long currentUserId = 10L; // 나
        Long authorId = 20L;      // 작성자
        Long challengeId = 100L;

        User author = createUser(authorId, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, author, VerificationStatus.COMPLETED);

        // 1. 게시글 조회 Mock
        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));

        // 2. 차단 관계 확인 Mock (서로 차단 안 함)
        given(userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, authorId)).willReturn(false);
        given(userBlockRepository.existsByBlockerIdAndBlockedId(authorId, currentUserId)).willReturn(false);

        // 3. 챌린지 참여 여부 Mock (참여 중 -> 댓글 작성 가능)
        given(userChallengeRepository.findByUserIdAndChallengeId(currentUserId, challengeId))
                .willReturn(Optional.of(UserChallenge.builder().id(999L).build()));

        // 4. 댓글 서비스 Mock
        CommentListResponseDto mockComments = CommentListResponseDto.builder()
                .comments(Collections.emptyList())
                .build();
        given(commentService.getComments(anyLong(), any(), any(Pageable.class)))
                .willReturn(mockComments);

        // 5. Converter Mock (인자 8개 맞춤, canWriteComment=true 예상)
        VerificationDetailResponseDto expectedDto = VerificationDetailResponseDto.builder()
                .verificationId(verificationId)
                .canWriteComment(true)
                .build();

		given(verificationConverter.toDetailDto(
			any(Verification.class),        // 1. Verification
			any(CommentListResponseDto.class), // 2. CommentListResponseDto
			anyBoolean(),                   // 3. isLiked
			anyBoolean(),                   // 4. canEdit
			anyBoolean(),                   // 5. canDelete
			anyBoolean(),                   // 6. canReport
			anyBoolean(),                       // 7. canWriteComment (핵심 검증 대상)
			any()                           // 8. currentUser (User 객체 혹은 null)
		)).willReturn(expectedDto);

        // when
        VerificationDetailResponseDto result = verificationService.getVerificationDetail(verificationId, currentUserId, 0, 10);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getVerificationId()).isEqualTo(verificationId);
        assertThat(result.isCanWriteComment()).isTrue();
    }

    @Test
    @DisplayName("성공: 챌린지 미참여자는 댓글 작성 불가 (canWriteComment=false)")
    void getVerificationDetail_Success_CannotWriteComment() {
        // given
        Long verificationId = 1L;
        Long currentUserId = 10L;
        Long authorId = 20L;
        Long challengeId = 100L;

        User author = createUser(authorId, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, author, VerificationStatus.COMPLETED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userBlockRepository.existsByBlockerIdAndBlockedId(anyLong(), anyLong())).willReturn(false);

		CommentListResponseDto mockComments = CommentListResponseDto.builder()
			.comments(Collections.emptyList()) // 빈 리스트라도 넣어줘야 NPE가 안 납니다.
			.build();
		given(commentService.getComments(anyLong(), any(), any(Pageable.class)))
			.willReturn(mockComments);

        // **핵심**: 챌린지 미참여 (Empty)
        given(userChallengeRepository.findByUserIdAndChallengeId(currentUserId, challengeId))
                .willReturn(Optional.empty());

        // Converter Mock (canWriteComment=false 예상)
        VerificationDetailResponseDto expectedDto = VerificationDetailResponseDto.builder()
                .verificationId(verificationId)
                .canWriteComment(false)
                .build();

		// Converter Mock 부분도 인자 개수 8개로 맞춤
		given(verificationConverter.toDetailDto(
			any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(),
			eq(false), // canWriteComment = false 예상
			any()
		)).willReturn(expectedDto);

        // when
        VerificationDetailResponseDto result = verificationService.getVerificationDetail(verificationId, currentUserId, 0, 10);

        // then
        assertThat(result.isCanWriteComment()).isFalse();
    }

    @Test
    @DisplayName("실패: 신고 누적으로 차단된(BLOCKED) 게시글 조회 시 예외 발생")
    void getVerificationDetail_Fail_BlockedPost() {
        // given
        Long verificationId = 1L;
        Long currentUserId = 10L;
        User author = createUser(20L, UserStatus.ACTIVE);

        // 게시글 상태가 BLOCKED
        Verification verification = createVerification(verificationId, author, VerificationStatus.BLOCKED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));

        // when & then
        assertThatThrownBy(() -> verificationService.getVerificationDetail(verificationId, currentUserId, 0, 10))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED_REPORTED_POST);
    }

    @Test
    @DisplayName("실패: 탈퇴한 유저(INACTIVE)의 글 조회 시 예외 발생 (마스킹)")
    void getVerificationDetail_Fail_InactiveUser() {
        // given
        Long verificationId = 1L;
        Long currentUserId = 10L;

        // 작성자 상태가 INACTIVE (탈퇴)
        User author = createUser(20L, UserStatus.INACTIVE);
        Verification verification = createVerification(verificationId, author, VerificationStatus.COMPLETED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));

        // when & then
        assertThatThrownBy(() -> verificationService.getVerificationDetail(verificationId, currentUserId, 0, 10))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: 내가 차단한 유저의 글 조회 시 예외 발생 (마스킹)")
    void getVerificationDetail_Fail_MyBlock() {
        // given
        Long verificationId = 1L;
        Long currentUserId = 10L; // 나
        Long authorId = 20L;      // 작성자

        User author = createUser(authorId, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, author, VerificationStatus.COMPLETED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));

        // 내가 작성자를 차단함 (Exists -> true)
        given(userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, authorId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> verificationService.getVerificationDetail(verificationId, currentUserId, 0, 10))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("실패: 나를 차단한 유저의 글 조회 시 예외 발생 (마스킹)")
    void getVerificationDetail_Fail_BlockedByAuthor() {
        // given
        Long verificationId = 1L;
        Long currentUserId = 10L; // 나
        Long authorId = 20L;      // 작성자

        User author = createUser(authorId, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, author, VerificationStatus.COMPLETED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));

        // 내가 차단한 건 아님
        given(userBlockRepository.existsByBlockerIdAndBlockedId(currentUserId, authorId)).willReturn(false);

        // 작성자가 나를 차단함 (Exists -> true)
        given(userBlockRepository.existsByBlockerIdAndBlockedId(authorId, currentUserId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> verificationService.getVerificationDetail(verificationId, currentUserId, 0, 10))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("성공: 인증 삭제 시, 인증 삭제보다 먼저 관련 포인트를 회수한다 (순서 검증)")
    void deleteVerification_revokesPointsBeforeDeleting() {
        // given
        Long verificationId = 1L;
        Long authorId = 10L;
        User author = createUser(authorId, UserStatus.ACTIVE);

        // 인증 요일/시간대 검증이 언제 실행해도 항상 통과하도록, 매일 + 하루종일 인증 가능한 챌린지로 구성
        Challenge challenge = Challenge.builder()
                .id(100L)
                .title("Test Challenge")
                .verifyStartTime(LocalTime.MIN)
                .verifyEndTime(LocalTime.MAX)
                .challengeDays(Arrays.stream(ChallengeDays.values())
                        .map(day -> ChallengeDayJoin.builder().dayOfWeek(day).build())
                        .toList())
                .build();
        Round round = Round.builder().id(200L).challenge(challenge).build();
        UserChallenge userChallenge = UserChallenge.builder().user(author).challenge(challenge).build();
        RoundRecord roundRecord = RoundRecord.builder().round(round).userChallenge(userChallenge).build();

        Verification verification = Verification.builder()
                .id(verificationId)
                .roundRecord(roundRecord)
                .status(VerificationStatus.COMPLETED)
                .isQuestion(false)
                .isResolved(false)
                .build();
        // createdAt은 BaseEntity 필드라 빌더로 설정이 안 되어, 인증 시간대 검증 통과를 위해 리플렉션으로 "지금"으로 세팅
        ReflectionTestUtils.setField(verification, "createdAt", LocalDateTime.now());

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));

        // when
        verificationService.deleteVerification(verificationId, authorId);

        // then: point_history가 verification을 FK로 참조하므로, 반드시 포인트 회수 -> 인증 삭제 순서여야 함
        InOrder inOrder = Mockito.inOrder(pointService, verificationRepository);
        inOrder.verify(pointService, times(1)).revokePointsForVerification(verification);
        inOrder.verify(verificationRepository, times(1)).delete(verification);
    }

    @Test
    @DisplayName("성공: 스크랩 등록은 INSERT IGNORE를 호출하고 isScrapped=true를 반환한다")
    void scrapVerification_usesInsertIgnoreAndReturnsScrapped() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.ACTIVE), VerificationStatus.COMPLETED);
        Round postRound = verification.getRoundRecord().getRound();
        Long challengeId = postRound.getChallenge().getId();
        UserChallenge userChallenge = UserChallenge.builder()
                .user(currentUser)
                .challenge(postRound.getChallenge())
                .build();
        RoundRecord userRoundRecord = RoundRecord.builder()
                .round(postRound)
                .userChallenge(userChallenge)
                .build();
        VerificationResponseDto.ScrapResponseDto expected = VerificationResponseDto.ScrapResponseDto.builder()
                .verificationId(verificationId)
                .isScrapped(true)
                .build();

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userChallengeRepository.findByUserIdAndChallengeId(currentUser.getId(), challengeId))
                .willReturn(Optional.of(userChallenge));
        given(roundRecordRepository.findByUserChallengeAndRoundId(userChallenge, postRound.getId()))
                .willReturn(Optional.of(userRoundRecord));
        given(verificationConverter.toScrapResponseDto(verification)).willReturn(expected);

        // when
        VerificationResponseDto.ScrapResponseDto result = verificationService.scrapVerification(verificationId, currentUser);

        // then
        assertThat(result.getVerificationId()).isEqualTo(verificationId);
        assertThat(result.getIsScrapped()).isTrue();
        Mockito.verify(verificationScrapRepository, times(1)).insertIgnore(currentUser.getId(), verificationId);
        Mockito.verify(verificationScrapRepository, Mockito.never()).save(any(VerificationScrap.class));
        Mockito.verify(verificationScrapRepository, Mockito.never()).flush();
    }

    @Test
    @DisplayName("실패: 차단된 인증글은 스크랩을 등록할 수 없다")
    void scrapVerification_whenVerificationIsBlocked_throwsAccessDeniedBeforeInsert() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.ACTIVE), VerificationStatus.BLOCKED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);

        // when & then
        assertThatThrownBy(() -> verificationService.scrapVerification(verificationId, currentUser))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED_REPORTED_POST);
        Mockito.verify(verificationScrapRepository, Mockito.never()).insertIgnore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 비활성 작성자의 인증글은 스크랩을 등록할 수 없다")
    void scrapVerification_whenAuthorIsInactive_throwsNotFoundBeforeInsert() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.INACTIVE), VerificationStatus.COMPLETED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);

        // when & then
        assertThatThrownBy(() -> verificationService.scrapVerification(verificationId, currentUser))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_NOT_FOUND);
        Mockito.verify(verificationScrapRepository, Mockito.never()).insertIgnore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 작성자와 차단 관계가 있으면 스크랩을 해제할 수 없다")
    void unscrapVerification_whenBlockedRelationExists_throwsNotFoundBeforeDelete() {
        // given
        Long verificationId = 125L;
        Long authorId = 20L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(authorId, UserStatus.ACTIVE), VerificationStatus.COMPLETED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);
        given(userBlockRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), authorId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> verificationService.unscrapVerification(verificationId, currentUser))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_NOT_FOUND);
        Mockito.verify(verificationScrapRepository, Mockito.never())
                .deleteByUserIdAndVerificationId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("성공: 좋아요가 없는 인증글에 좋아요를 등록하면 INSERT IGNORE를 호출하고 isLiked=true를 반환한다")
    void likeVerification_usesInsertIgnoreAndReturnsLiked() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.ACTIVE), VerificationStatus.COMPLETED);
        VerificationResponseDto.LikeResponseDto expected = VerificationResponseDto.LikeResponseDto.builder()
                .verificationId(verificationId)
                .isLiked(true)
                .build();

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);
        given(verificationConverter.toLikeResponseDto(verification)).willReturn(expected);

        // when
        VerificationResponseDto.LikeResponseDto result = verificationService.likeVerification(verificationId, currentUser);

        // then
        assertThat(result.getVerificationId()).isEqualTo(verificationId);
        assertThat(result.getIsLiked()).isTrue();
        Mockito.verify(verificationLikeRepository, times(1)).insertIgnore(currentUser.getId(), verificationId);
        Mockito.verify(verificationLikeRepository, Mockito.never()).save(any(VerificationLike.class));
        Mockito.verify(verificationLikeRepository, Mockito.never()).flush();
    }

    @Test
    @DisplayName("실패: 차단된 인증글은 좋아요를 등록할 수 없다")
    void likeVerification_whenVerificationIsBlocked_throwsAccessDeniedBeforeInsert() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.ACTIVE), VerificationStatus.BLOCKED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);

        // when & then
        assertThatThrownBy(() -> verificationService.likeVerification(verificationId, currentUser))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCESS_DENIED_REPORTED_POST);
        Mockito.verify(verificationLikeRepository, Mockito.never()).insertIgnore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 비활성 작성자의 인증글은 좋아요를 등록할 수 없다")
    void likeVerification_whenAuthorIsInactive_throwsNotFoundBeforeInsert() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.INACTIVE), VerificationStatus.COMPLETED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);

        // when & then
        assertThatThrownBy(() -> verificationService.likeVerification(verificationId, currentUser))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_NOT_FOUND);
        Mockito.verify(verificationLikeRepository, Mockito.never()).insertIgnore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 작성자와 차단 관계가 있으면 좋아요를 등록할 수 없다")
    void likeVerification_whenBlockedRelationExists_throwsNotFoundBeforeInsert() {
        // given
        Long verificationId = 125L;
        Long authorId = 20L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(authorId, UserStatus.ACTIVE), VerificationStatus.COMPLETED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);
        given(userBlockRepository.existsByBlockerIdAndBlockedId(authorId, currentUser.getId())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> verificationService.likeVerification(verificationId, currentUser))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_NOT_FOUND);
        Mockito.verify(verificationLikeRepository, Mockito.never()).insertIgnore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("성공: 이미 좋아요한 인증글에 다시 요청해도 중복 저장 없이 isLiked=true를 반환한다")
    void likeVerification_whenAlreadyLiked_returnsLikedWithoutDuplicateSave() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.ACTIVE), VerificationStatus.COMPLETED);
        VerificationResponseDto.LikeResponseDto expected = VerificationResponseDto.LikeResponseDto.builder()
                .verificationId(verificationId)
                .isLiked(true)
                .build();

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);
        given(verificationConverter.toLikeResponseDto(verification)).willReturn(expected);

        // when
        VerificationResponseDto.LikeResponseDto firstResult = verificationService.likeVerification(verificationId, currentUser);
        VerificationResponseDto.LikeResponseDto secondResult = verificationService.likeVerification(verificationId, currentUser);

        // then
        assertThat(firstResult.getIsLiked()).isTrue();
        assertThat(secondResult.getIsLiked()).isTrue();
        Mockito.verify(verificationLikeRepository, times(2)).insertIgnore(currentUser.getId(), verificationId);
        Mockito.verify(verificationLikeRepository, Mockito.never()).save(any(VerificationLike.class));
        Mockito.verify(verificationLikeRepository, Mockito.never()).flush();
    }

    @Test
    @DisplayName("실패: 존재하지 않는 인증글 좋아요 등록 요청은 VERIFICATION_NOT_FOUND를 반환한다")
    void likeVerification_whenVerificationDoesNotExist_throwsNotFound() {
        // given
        Long verificationId = 999L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> verificationService.likeVerification(verificationId, currentUser))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_NOT_FOUND);
        Mockito.verify(verificationLikeRepository, Mockito.never()).insertIgnore(anyLong(), anyLong());
        Mockito.verify(verificationLikeRepository, Mockito.never()).save(any(VerificationLike.class));
    }

    @Test
    @DisplayName("실패: 인증글이 속한 라운드에 참여하지 않은 사용자는 좋아요를 등록할 수 없다")
    void likeVerification_whenUserDidNotParticipateInRound_throwsAccessDeniedBeforeInsert() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.ACTIVE), VerificationStatus.COMPLETED);
        Round postRound = verification.getRoundRecord().getRound();
        Long challengeId = postRound.getChallenge().getId();
        UserChallenge userChallenge = UserChallenge.builder()
                .user(currentUser)
                .challenge(postRound.getChallenge())
                .build();

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userChallengeRepository.findByUserIdAndChallengeId(currentUser.getId(), challengeId))
                .willReturn(Optional.of(userChallenge));
        given(roundRecordRepository.findByUserChallengeAndRoundId(userChallenge, postRound.getId()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> verificationService.likeVerification(verificationId, currentUser))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_ACCESS_DENIED);
        Mockito.verify(verificationLikeRepository, Mockito.never()).insertIgnore(anyLong(), anyLong());
    }

    @Test
    @DisplayName("성공: 동일 사용자와 동일 인증글 조합은 repository save가 아니라 INSERT IGNORE로만 처리한다")
    void likeVerification_sameUserAndVerification_doesNotUseJpaSavePath() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.ACTIVE), VerificationStatus.COMPLETED);
        VerificationResponseDto.LikeResponseDto expected = VerificationResponseDto.LikeResponseDto.builder()
                .verificationId(verificationId)
                .isLiked(true)
                .build();

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);
        given(verificationConverter.toLikeResponseDto(verification)).willReturn(expected);

        // when
        verificationService.likeVerification(verificationId, currentUser);
        verificationService.likeVerification(verificationId, currentUser);

        // then
        Mockito.verify(verificationLikeRepository, times(2)).insertIgnore(currentUser.getId(), verificationId);
        Mockito.verify(verificationLikeRepository, Mockito.never()).save(any(VerificationLike.class));
        Mockito.verify(verificationLikeRepository, Mockito.never()).flush();
    }

    @Test
    @DisplayName("성공: 좋아요가 존재하는 인증글에 좋아요 취소를 요청하면 삭제하고 isLiked=false를 반환한다")
    void unlikeVerification_deletesLikeAndReturnsUnliked() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.ACTIVE), VerificationStatus.COMPLETED);
        VerificationResponseDto.LikeResponseDto expected = VerificationResponseDto.LikeResponseDto.builder()
                .verificationId(verificationId)
                .isLiked(false)
                .build();

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);
        given(verificationConverter.toLikeResponseDto(verification, false)).willReturn(expected);

        // when
        VerificationResponseDto.LikeResponseDto result = verificationService.unlikeVerification(verificationId, currentUser);

        // then
        assertThat(result.getVerificationId()).isEqualTo(verificationId);
        assertThat(result.getIsLiked()).isFalse();
        Mockito.verify(verificationLikeRepository, times(1))
                .deleteByUserIdAndVerificationId(currentUser.getId(), verificationId);
    }

    @Test
    @DisplayName("성공: 좋아요 데이터가 없어도 좋아요 취소는 예외 없이 isLiked=false를 반환한다")
    void unlikeVerification_succeedsWhenLikeDoesNotExist() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.ACTIVE), VerificationStatus.COMPLETED);
        VerificationResponseDto.LikeResponseDto expected = VerificationResponseDto.LikeResponseDto.builder()
                .verificationId(verificationId)
                .isLiked(false)
                .build();

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);
        given(verificationConverter.toLikeResponseDto(verification, false)).willReturn(expected);

        // when
        VerificationResponseDto.LikeResponseDto result = verificationService.unlikeVerification(verificationId, currentUser);

        // then
        assertThat(result.getVerificationId()).isEqualTo(verificationId);
        assertThat(result.getIsLiked()).isFalse();
        Mockito.verify(verificationLikeRepository, times(1))
                .deleteByUserIdAndVerificationId(currentUser.getId(), verificationId);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 인증글 좋아요 취소 요청은 VERIFICATION_NOT_FOUND를 반환한다")
    void unlikeVerification_whenVerificationDoesNotExist_throwsNotFound() {
        // given
        Long verificationId = 999L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> verificationService.unlikeVerification(verificationId, currentUser))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_NOT_FOUND);
        Mockito.verify(verificationLikeRepository, Mockito.never())
                .deleteByUserIdAndVerificationId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 인증글이 속한 라운드에 참여하지 않은 사용자는 좋아요를 취소할 수 없다")
    void unlikeVerification_whenUserDidNotParticipateInRound_throwsAccessDeniedBeforeDelete() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.ACTIVE), VerificationStatus.COMPLETED);
        Round postRound = verification.getRoundRecord().getRound();
        Long challengeId = postRound.getChallenge().getId();
        UserChallenge userChallenge = UserChallenge.builder()
                .user(currentUser)
                .challenge(postRound.getChallenge())
                .build();

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userChallengeRepository.findByUserIdAndChallengeId(currentUser.getId(), challengeId))
                .willReturn(Optional.of(userChallenge));
        given(roundRecordRepository.findByUserChallengeAndRoundId(userChallenge, postRound.getId()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> verificationService.unlikeVerification(verificationId, currentUser))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_ACCESS_DENIED);
        Mockito.verify(verificationLikeRepository, Mockito.never())
                .deleteByUserIdAndVerificationId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("실패: 작성자와 차단 관계가 있으면 좋아요를 취소할 수 없다")
    void unlikeVerification_whenBlockedRelationExists_throwsNotFoundBeforeDelete() {
        // given
        Long verificationId = 125L;
        Long authorId = 20L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(authorId, UserStatus.ACTIVE), VerificationStatus.COMPLETED);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        givenRoundParticipation(verification, currentUser);
        given(userBlockRepository.existsByBlockerIdAndBlockedId(currentUser.getId(), authorId)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> verificationService.unlikeVerification(verificationId, currentUser))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_NOT_FOUND);
        Mockito.verify(verificationLikeRepository, Mockito.never())
                .deleteByUserIdAndVerificationId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("성공: 스크랩 데이터가 없어도 스크랩 해제는 예외 없이 미스크랩 상태를 반환한다")
    void unscrapVerification_succeedsWhenScrapDoesNotExist() {
        // given
        Long verificationId = 125L;
        User currentUser = createUser(10L, UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId, createUser(20L, UserStatus.ACTIVE), VerificationStatus.COMPLETED);
        Round postRound = verification.getRoundRecord().getRound();
        Long challengeId = postRound.getChallenge().getId();
        UserChallenge userChallenge = UserChallenge.builder()
                .user(currentUser)
                .challenge(postRound.getChallenge())
                .build();
        RoundRecord userRoundRecord = RoundRecord.builder()
                .round(postRound)
                .userChallenge(userChallenge)
                .build();
        VerificationResponseDto.ScrapResponseDto expected = VerificationResponseDto.ScrapResponseDto.builder()
                .verificationId(verificationId)
                .isScrapped(false)
                .build();

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userChallengeRepository.findByUserIdAndChallengeId(currentUser.getId(), challengeId))
                .willReturn(Optional.of(userChallenge));
        given(roundRecordRepository.findByUserChallengeAndRoundId(userChallenge, postRound.getId()))
                .willReturn(Optional.of(userRoundRecord));
        given(verificationConverter.toScrapResponseDto(verification, false)).willReturn(expected);

        // when
        VerificationResponseDto.ScrapResponseDto result = verificationService.unscrapVerification(verificationId, currentUser);

        // then
        assertThat(result.getVerificationId()).isEqualTo(verificationId);
        assertThat(result.getIsScrapped()).isFalse();
        Mockito.verify(verificationScrapRepository, times(1))
                .deleteByUserIdAndVerificationId(currentUser.getId(), verificationId);
    }
}
