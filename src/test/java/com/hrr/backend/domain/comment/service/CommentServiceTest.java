package com.hrr.backend.domain.comment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.comment.dto.CommentCreateRequestDto;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.hrr.backend.domain.comment.converter.CommentConverter;
import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.comment.dto.CommentResponseDto;
import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.comment.entity.enums.CommentMaskingType;
import com.hrr.backend.domain.comment.repository.CommentRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.user.repository.UserBlockRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.s3.S3UrlUtil;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    private CommentServiceImpl commentService;
    private CommentConverter commentConverter;

    @Mock private VerificationRepository verificationRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserBlockRepository userBlockRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private S3UrlUtil s3UrlUtil;

    @BeforeEach
    void setUp() {
        // Converter는 로직이 복잡하므로 Mock이 아닌 실제 객체를 사용하여 결과 검증
        commentConverter = new CommentConverter(s3UrlUtil);

        // Service 생성 (생성자 주입)
        commentService = new CommentServiceImpl(
                verificationRepository,
                commentRepository,
                userRepository,
                commentConverter,
                userBlockRepository,
                userChallengeRepository,
                null // RoundRepository는 createComment에서 직접 사용하지 않음 (Challenge를 통해 접근)
        );
    }

    // --- 헬퍼 메서드 ---

    private User createUser(Long id, String nickname, UserStatus status) {
        return User.builder()
                .id(id)
                .nickname(nickname)
                .userStatus(status)
                .profileImage("profile.jpg")
                .build();
    }

    private Verification createVerification(Long id) {
        return Verification.builder()
                .id(id)
                .build();
    }

    /**
     * Comment 객체 생성 및 Reflection을 이용한 BaseEntity 필드 주입
     */
    private Comment createComment(Long id, Verification verification, User user, String content, boolean isDeleted, boolean isAnonymous, Integer anonymousNumber) {
        Comment comment = Comment.builder()
                .id(id)
                .verification(verification)
                .user(user)
                .content(content)
                .isDeleted(isDeleted)
                .isAnonymous(isAnonymous)
                .anonymousNumber(anonymousNumber)
                .likesCount(0)
                .depth(0)
                .build();

        // BaseEntity 필드 주입 (생성 시간 정렬 테스트 시 필요)
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());

        return comment;
    }

    // --- 테스트 케이스 (조회) ---

    @Test
    @DisplayName("정상 조회: 차단/삭제/탈퇴 없는 클린한 댓글")
    void getComments_Normal() {
        // given
        Long verificationId = 100L;
        Long myId = 1L;
        Long writerId = 2L;

        User me = createUser(myId, "나", UserStatus.ACTIVE);
        User writer = createUser(writerId, "작성자", UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId);

        Comment comment = createComment(10L, verification, writer, "정상 댓글입니다.", false, false, null);

        // Mocking
        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(myId)).willReturn(Optional.of(me));

        // 차단 목록 없음
        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());

        // 댓글 조회 (삭제된 것도 포함하는 메서드 호출됨)
        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));

        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
                .willReturn(Collections.emptyList());

        given(s3UrlUtil.toFullUrl(anyString())).willReturn("http://full-url.com/profile.jpg");

        // [추가] 전체 카운트 Mock
        given(commentRepository.countByVerification(verification)).willReturn(1L);

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);
        assertThat(dto.getContent()).isEqualTo("정상 댓글입니다.");
        assertThat(dto.getUserName()).isEqualTo("작성자");
        assertThat(dto.getMaskingType()).isEqualTo(CommentMaskingType.NONE);
        assertThat(dto.getUserProfileUrl()).isEqualTo("http://full-url.com/profile.jpg");
        assertThat(result.getTotalCount()).isEqualTo(1L); // 전체 개수 검증
    }

    @Test
    @DisplayName("마스킹: 내가 차단한 사용자의 댓글 (Type: BLOCKED)")
    void getComments_BlockedUser() {
        // given
        Long verificationId = 100L;
        Long myId = 1L;
        Long blockedUserId = 99L;

        User me = createUser(myId, "나", UserStatus.ACTIVE);
        User blockedUser = createUser(blockedUserId, "차단남", UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId);

        Comment comment = createComment(11L, verification, blockedUser, "보기 싫은 글", false, false, null);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(myId)).willReturn(Optional.of(me));

        // [중요] 내가 차단한 목록에 포함됨 -> 마스킹 대상
        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(List.of(blockedUserId));

        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));

        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
                .willReturn(Collections.emptyList());

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);
        assertThat(dto.getContent()).isEqualTo("차단된 사용자의 댓글입니다.");
        assertThat(dto.getUserName()).isNull(); // Converter 로직 상 null
        assertThat(dto.getMaskingType()).isEqualTo(CommentMaskingType.BLOCKED);
        assertThat(dto.getUserId()).isNull(); // ID 노출 X
    }

    @Test
    @DisplayName("마스킹 X: 나를 차단한 사용자의 댓글 (Type: NONE) - 단방향 차단 검증")
    void getComments_UserWhoBlockedMe() {
        // given
        Long verificationId = 100L;
        Long myId = 1L;
        Long blockerId = 99L; // 나를 차단한 사람

        User me = createUser(myId, "나", UserStatus.ACTIVE);
        User blocker = createUser(blockerId, "나를차단한사람", UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId);

        Comment comment = createComment(11L, verification, blocker, "나는 보여야 해", false, false, null);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(myId)).willReturn(Optional.of(me));

        // [중요] 내가 차단한 목록은 비어있음 (상대가 나를 차단한 건 조회 시 영향 X)
        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());

        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));

        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
                .willReturn(Collections.emptyList());

        given(s3UrlUtil.toFullUrl(anyString())).willReturn("http://full-url.com/profile.jpg");

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);
        assertThat(dto.getContent()).isEqualTo("나는 보여야 해"); // 마스킹 되지 않음
        assertThat(dto.getUserName()).isEqualTo("나를차단한사람");
        assertThat(dto.getMaskingType()).isEqualTo(CommentMaskingType.NONE);
    }

    @Test
    @DisplayName("마스킹: 탈퇴한 사용자 (Type: INACTIVE) - 닉네임 변경, 내용 유지")
    void getComments_InactiveUser() {
        // given
        Long verificationId = 100L;
        Long myId = 1L;

        User me = createUser(myId, "나", UserStatus.ACTIVE);
        // 상태가 INACTIVE인 사용자
        User inactiveUser = createUser(50L, "탈퇴자", UserStatus.INACTIVE);
        Verification verification = createVerification(verificationId);

        Comment comment = createComment(12L, verification, inactiveUser, "탈퇴 전 남긴 소중한 기록", false, false, null);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(myId)).willReturn(Optional.of(me));
        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());

        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));

        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
                .willReturn(Collections.emptyList());

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);

        // [검증 포인트] 탈퇴한 사용자는 내용은 유지, 닉네임만 변경
        assertThat(dto.getContent()).isEqualTo("탈퇴 전 남긴 소중한 기록");
        assertThat(dto.getUserName()).isEqualTo("탈퇴한 사용자");
        assertThat(dto.getMaskingType()).isEqualTo(CommentMaskingType.INACTIVE);
        assertThat(dto.getUserId()).isNull(); // ID 노출 X
    }

    @Test
    @DisplayName("마스킹: 삭제된 댓글 (Type: DELETED) - Soft Delete")
    void getComments_Deleted() {
        // given
        Long verificationId = 100L;
        Long myId = 1L;

        User me = createUser(myId, "나", UserStatus.ACTIVE);
        User writer = createUser(2L, "작성자", UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId);

        // isDeleted = true
        Comment comment = createComment(13L, verification, writer, "삭제된 글", true, false, null);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(myId)).willReturn(Optional.of(me));
        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());

        // 삭제된 댓글도 조회됨 (대댓글 구조 유지를 위해)
        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));

        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
                .willReturn(Collections.emptyList());

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);
        assertThat(dto.getContent()).isEqualTo("삭제된 댓글입니다.");
        assertThat(dto.getUserName()).isEqualTo("삭제");
        assertThat(dto.getMaskingType()).isEqualTo(CommentMaskingType.DELETED);
    }

    @Test
    @DisplayName("익명 댓글 조회")
    void getComments_Anonymous() {
        // given
        Long verificationId = 100L;
        Long myId = 1L;
        Long otherId = 2L;

        User me = createUser(myId, "나", UserStatus.ACTIVE);
        User other = createUser(otherId, "타인", UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId);

        // 익명, anonymousNumber=3
        Comment comment = createComment(14L, verification, other, "비밀글입니다", false, true, 3);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(myId)).willReturn(Optional.of(me));
        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());

        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));
        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
                .willReturn(Collections.emptyList());

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);
        assertThat(dto.getContent()).isEqualTo("비밀글입니다");
        assertThat(dto.getUserName()).isEqualTo("익명3");
        assertThat(dto.getUserId()).isNull();
        assertThat(dto.isAnonymous()).isTrue();
    }

    // --- [추가된 테스트] 댓글 작성 검증 (Review 반영) ---

    @Test
    @DisplayName("댓글 작성 실패: 챌린지 미참여 유저 (USER_CHALLENGE_NOT_FOUND)")
    void createComment_UserChallengeNotFound() {
        // given
        Long verificationId = 100L;
        Long userId = 1L;
        Long challengeId = 10L;

        // Mock 객체 체이닝을 위한 설정
        Verification verification = mock(Verification.class);
        RoundRecord roundRecord = mock(RoundRecord.class);
        Round round = mock(Round.class);
        Challenge challenge = mock(Challenge.class);
        User user = createUser(userId, "참여자", UserStatus.ACTIVE);

        // 체이닝 연결
        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(verification.getRoundRecord()).willReturn(roundRecord);
        given(roundRecord.getRound()).willReturn(round);
        given(round.getChallenge()).willReturn(challenge);
        given(challenge.getId()).willReturn(challengeId);

        // [핵심] 유저 챌린지 정보가 없음 (미참여)
        given(userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId))
                .willReturn(Optional.empty());

        CommentCreateRequestDto request = new CommentCreateRequestDto();

        // when & then
        assertThatThrownBy(() -> commentService.createComment(verificationId, userId, request))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_CHALLENGE_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 작성 실패: 참여는 했으나 상태가 JOINED가 아님 (DROP/QUIT 등)")
    void createComment_UserChallengeNotJoined() {
        // given
        Long verificationId = 100L;
        Long userId = 1L;
        Long challengeId = 10L;

        Verification verification = mock(Verification.class);
        RoundRecord roundRecord = mock(RoundRecord.class);
        Round round = mock(Round.class);
        Challenge challenge = mock(Challenge.class);
        User user = createUser(userId, "중도포기자", UserStatus.ACTIVE);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(verification.getRoundRecord()).willReturn(roundRecord);
        given(roundRecord.getRound()).willReturn(round);
        given(round.getChallenge()).willReturn(challenge);
        given(challenge.getId()).willReturn(challengeId);

        // [핵심] 참여 상태가 DROP (중도 포기)
        UserChallenge userChallenge = mock(UserChallenge.class);
        given(userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId))
                .willReturn(Optional.of(userChallenge));
        given(userChallenge.getStatus()).willReturn(ChallengeJoinStatus.DROPPED);

        CommentCreateRequestDto request = new CommentCreateRequestDto();

        // when & then
        assertThatThrownBy(() -> commentService.createComment(verificationId, userId, request))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_CHALLENGE_NOT_FOUND);
    }

    @Test
    @DisplayName("댓글 작성 실패: 현재 진행 중인 라운드가 아닌 인증글 (ROUND_NOT_CURRENT)")
    void createComment_RoundNotCurrent() {
        // given
        Long verificationId = 100L;
        Long userId = 1L;
        Long challengeId = 10L;
        Long currentRoundId = 50L;
        Long pastRoundId = 49L;

        Verification verification = mock(Verification.class);
        RoundRecord roundRecord = mock(RoundRecord.class);
        Round round = mock(Round.class);
        Challenge challenge = mock(Challenge.class);
        Round currentRound = mock(Round.class); // 현재 진행중인 라운드 객체
        User user = createUser(userId, "참여자", UserStatus.ACTIVE);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // 체이닝 & 상태 설정
        given(verification.getRoundRecord()).willReturn(roundRecord);
        given(roundRecord.getRound()).willReturn(round);
        given(round.getChallenge()).willReturn(challenge);
        given(challenge.getId()).willReturn(challengeId);

        // 유저는 정상 참여 상태
        UserChallenge userChallenge = mock(UserChallenge.class);
        given(userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId))
                .willReturn(Optional.of(userChallenge));
        given(userChallenge.getStatus()).willReturn(ChallengeJoinStatus.JOINED);

        // 챌린지의 현재 라운드 vs 인증글의 라운드 불일치
        given(challenge.getCurrentRound()).willReturn(currentRound);
        given(currentRound.getId()).willReturn(currentRoundId);
        given(verification.getRoundId()).willReturn(pastRoundId); // 과거 라운드

        CommentCreateRequestDto request = new CommentCreateRequestDto();

        // when & then
        assertThatThrownBy(() -> commentService.createComment(verificationId, userId, request))
                .isInstanceOf(GlobalException.class)
                // ErrorCode가 업데이트 되었는지 확인 (ROUND4006)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROUND_NOT_CURRENT);
    }
}