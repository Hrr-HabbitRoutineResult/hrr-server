package com.hrr.backend.domain.verification.service;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.comment.service.CommentService;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.user.repository.UserBlockRepository;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.verification.converter.VerificationConverter;
import com.hrr.backend.domain.verification.dto.VerificationDetailResponseDto;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
}
