//package com.hrr.backend.domain.comment.service;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.*;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.PageImpl;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import com.hrr.backend.domain.comment.converter.CommentConverter;
//import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
//import com.hrr.backend.domain.comment.dto.CommentResponseDto;
//import com.hrr.backend.domain.comment.entity.Comment;
//import com.hrr.backend.domain.comment.repository.CommentRepository;
//import com.hrr.backend.domain.user.entity.User;
//import com.hrr.backend.domain.user.entity.enums.UserStatus;
//import com.hrr.backend.domain.user.repository.UserBlockRepository;
//import com.hrr.backend.domain.user.repository.UserRepository;
//import com.hrr.backend.domain.verification.entity.Verification;
//import com.hrr.backend.domain.verification.repository.VerificationRepository;
//import com.hrr.backend.global.s3.S3UrlUtil;
//
//@ExtendWith(MockitoExtension.class)
//class CommentServiceTest {
//
//    // 테스트 대상 Service
//    private CommentServiceImpl commentService;
//
//    // 실제 로직을 태울 Converter (Spy 대신 직접 생성)
//    private CommentConverter commentConverter;
//
//    @Mock
//    private VerificationRepository verificationRepository;
//    @Mock
//    private CommentRepository commentRepository;
//    @Mock
//    private UserRepository userRepository;
//    @Mock
//    private UserBlockRepository userBlockRepository;
//    @Mock
//    private S3UrlUtil s3UrlUtil;
//
//    @BeforeEach
//    void setUp() {
//        // [핵심 해결책]
//        // @Spy, @InjectMocks 의존성 주입 실패를 방지하기 위해
//        // @BeforeEach에서 명시적으로 객체를 조립합니다. (보내주신 다른 테스트 코드 스타일 참고)
//
//        // 1. Mock S3UrlUtil을 넣어서 Converter 생성
//        commentConverter = new CommentConverter(s3UrlUtil);
//
//        // 2. 생성된 Converter와 다른 Mock Repository들을 넣어서 Service 생성
//        commentService = new CommentServiceImpl(
//                verificationRepository,
//                commentRepository,   // ★ 얘를 두 번째로 올리고
//                userRepository,
//                commentConverter,
//                userBlockRepository
//        );
//    }
//
//    // --- 테스트 헬퍼 메서드 ---
//    private User createUser(Long id, String nickname, UserStatus status) {
//        return User.builder()
//                .id(id)
//                .nickname(nickname)
//                .userStatus(status)
//                .profileImage("profile.jpg")
//                .build();
//    }
//
//    @Test
//    @DisplayName("정상 조회: 차단/삭제/탈퇴 없는 클린한 댓글 조회")
//    void getComments_Normal() {
//        // given
//        Long verificationId = 100L;
//        Long myId = 1L;
//        Long writerId = 2L;
//
//        User me = createUser(myId, "나", UserStatus.ACTIVE);
//        User writer = createUser(writerId, "작성자", UserStatus.ACTIVE);
//        Verification verification = Verification.builder().id(verificationId).build();
//
//        Comment comment = Comment.builder()
//                .id(10L)
//                .verification(verification)
//                .user(writer)
//                .content("정상 댓글입니다.")
//                .isDeleted(false)
//                .isAnonymous(false)
//                .likesCount(0)
//                .build();
//
//        // Repository Mocking
//        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
//        given(userRepository.findById(myId)).willReturn(Optional.of(me));
//
//        // 차단 관계 없음 (최적화 메서드 호출 검증)
//        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());
//        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());
//
//        // 댓글 조회 결과
//        given(commentRepository.findByVerificationAndDepthAndIsDeletedFalseOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
//                .willReturn(new PageImpl<>(List.of(comment)));
//        given(commentRepository.findByParentInAndIsDeletedFalseOrderByCreatedAtAsc(anyList()))
//                .willReturn(Collections.emptyList());
//
//        // S3 Url 변환 Mocking
//        given(s3UrlUtil.toFullUrl(anyString())).willReturn("http://full-url.com/profile.jpg");
//
//        // when
//        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));
//
//        // then
//        CommentResponseDto dto = result.getComments().get(0);
//        assertThat(dto.getContent()).isEqualTo("정상 댓글입니다.");
//        assertThat(dto.getUserName()).isEqualTo("작성자");
//        assertThat(dto.getUserId()).isEqualTo(writerId); // ID 노출
//    }
//
//    @Test
//    @DisplayName("마스킹: 차단된 사용자(내가 차단함 + 나를 차단함)")
//    void getComments_BlockedUser() {
//        // given
//        Long verificationId = 100L;
//        Long myId = 1L;
//        Long blockedUserId = 99L; // 차단 대상
//
//        User me = createUser(myId, "나", UserStatus.ACTIVE);
//        User blockedUser = createUser(blockedUserId, "차단남", UserStatus.ACTIVE);
//        Verification verification = Verification.builder().id(verificationId).build();
//
//        Comment comment = Comment.builder()
//                .id(11L)
//                .verification(verification)
//                .user(blockedUser)
//                .content("차단된 유저의 글")
//                .isDeleted(false)
//                .build();
//
//        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
//        given(userRepository.findById(myId)).willReturn(Optional.of(me));
//
//        // [중요] 차단 목록에 ID 포함 (최적화 쿼리 사용 확인)
//        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(List.of(blockedUserId));
//        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());
//
//        given(commentRepository.findByVerificationAndDepthAndIsDeletedFalseOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
//                .willReturn(new PageImpl<>(List.of(comment)));
//
//        // when
//        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));
//
//        // then
//        CommentResponseDto dto = result.getComments().get(0);
//        assertThat(dto.getContent()).isEqualTo("차단된 사용자의 댓글입니다."); // 마스킹 텍스트 확인
//        assertThat(dto.getUserName()).isEqualTo("(알 수 없음)");
//        assertThat(dto.getUserId()).isNull(); // ID 숨김
//    }
//
//    @Test
//    @DisplayName("마스킹: 탈퇴한 사용자 (Inactive Status)")
//    void getComments_InactiveUser() {
//        // given
//        Long verificationId = 100L;
//        Long myId = 1L;
//
//        User me = createUser(myId, "나", UserStatus.ACTIVE);
//        // UserStatus가 INACTIVE
//        User inactiveUser = createUser(50L, "탈퇴자", UserStatus.INACTIVE);
//        Verification verification = Verification.builder().id(verificationId).build();
//
//        Comment comment = Comment.builder()
//                .id(12L)
//                .verification(verification)
//                .user(inactiveUser)
//                .content("탈퇴 전 남긴 글")
//                .isDeleted(false)
//                .build();
//
//        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
//        given(userRepository.findById(myId)).willReturn(Optional.of(me));
//        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());
//        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());
//
//        given(commentRepository.findByVerificationAndDepthAndIsDeletedFalseOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
//                .willReturn(new PageImpl<>(List.of(comment)));
//
//        // when
//        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));
//
//        // then
//        CommentResponseDto dto = result.getComments().get(0);
//        assertThat(dto.getContent()).isEqualTo("탈퇴한 사용자의 댓글입니다."); // 마스킹 확인
//        assertThat(dto.getUserName()).isEqualTo("(알 수 없음)");
//        assertThat(dto.getUserId()).isNull();
//    }
//
//    @Test
//    @DisplayName("마스킹: 삭제된 댓글 (Soft Delete)")
//    void getComments_Deleted() {
//        // given
//        Long verificationId = 100L;
//        Long myId = 1L;
//
//        User me = createUser(myId, "나", UserStatus.ACTIVE);
//        User writer = createUser(2L, "작성자", UserStatus.ACTIVE);
//        Verification verification = Verification.builder().id(verificationId).build();
//
//        Comment comment = Comment.builder()
//                .id(13L)
//                .verification(verification)
//                .user(writer)
//                .content("삭제되어 안 보이는 글")
//                .isDeleted(true) // 삭제 상태
//                .build();
//
//        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
//        given(userRepository.findById(myId)).willReturn(Optional.of(me));
//        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());
//        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());
//
//        // Repository가 삭제된 댓글도 가져온다고 가정 (대댓글 구조 등을 위해)
//        given(commentRepository.findByVerificationAndDepthAndIsDeletedFalseOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
//                .willReturn(new PageImpl<>(List.of(comment)));
//
//        // when
//        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));
//
//        // then
//        CommentResponseDto dto = result.getComments().get(0);
//        assertThat(dto.getContent()).isEqualTo("삭제된 댓글입니다."); // 마스킹 확인
//        assertThat(dto.getUserName()).isEqualTo("삭제"); // Converter 로직에 따라 변경
//        assertThat(dto.getUserId()).isNull();
//    }
//
//    @Test
//    @DisplayName("익명: 타인의 익명 댓글은 닉네임과 ID가 가려져야 함")
//    void getComments_Anonymous_Other() {
//        // given
//        Long verificationId = 100L;
//        Long myId = 1L;
//        Long otherId = 2L;
//
//        User me = createUser(myId, "나", UserStatus.ACTIVE);
//        User other = createUser(otherId, "타인", UserStatus.ACTIVE);
//        Verification verification = Verification.builder().id(verificationId).build();
//
//        Comment comment = Comment.builder()
//                .id(14L)
//                .verification(verification)
//                .user(other)
//                .content("비밀글입니다")
//                .isDeleted(false)
//                .isAnonymous(true)
//                .anonymousNumber(3)
//                .build();
//
//        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
//        given(userRepository.findById(myId)).willReturn(Optional.of(me));
//        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());
//        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());
//
//        given(commentRepository.findByVerificationAndDepthAndIsDeletedFalseOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
//                .willReturn(new PageImpl<>(List.of(comment)));
//
//        // when
//        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));
//
//        // then
//        CommentResponseDto dto = result.getComments().get(0);
//        assertThat(dto.getContent()).isEqualTo("비밀글입니다"); // 내용은 보임
//        assertThat(dto.getUserName()).isEqualTo("익명3"); // 닉네임 마스킹
//        assertThat(dto.getUserId()).isNull(); // ID 숨김
//        assertThat(dto.isAnonymous()).isTrue();
//    }
//}

package com.hrr.backend.domain.comment.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils; // 핵심 import

import com.hrr.backend.domain.comment.converter.CommentConverter;
import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.comment.dto.CommentResponseDto;
import com.hrr.backend.domain.comment.entity.Comment;
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
    @Mock private S3UrlUtil s3UrlUtil;

    @BeforeEach
    void setUp() {
        // Converter는 실제 로직 동작 확인을 위해 직접 생성
        commentConverter = new CommentConverter(s3UrlUtil);

        // Service 생성 (생성자 주입)
        commentService = new CommentServiceImpl(
                verificationRepository,
                commentRepository,
                userRepository,
                commentConverter,
                userBlockRepository
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
     * [핵심 해결] Reflection을 사용하여 Comment 객체 생성 및 createdAt 주입
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
                // .createdAt(LocalDateTime.now())  <-- 여기서 에러 나던 것을 제거
                .build();

        // 부모 클래스(BaseEntity)의 필드는 Reflection으로 주입
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());

        return comment;
    }

    // --- 테스트 케이스 ---

    @Test
    @DisplayName("정상 조회: 차단/삭제/탈퇴 없는 클린한 댓글 조회")
    void getComments_Normal() {
        // given
        Long verificationId = 100L;
        Long myId = 1L;
        Long writerId = 2L;

        User me = createUser(myId, "나", UserStatus.ACTIVE);
        User writer = createUser(writerId, "작성자", UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId);

        // 헬퍼 메서드로 Comment 생성 (Reflection 적용됨)
        Comment comment = createComment(10L, verification, writer, "정상 댓글입니다.", false, false, null);

        // Mocking
        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(myId)).willReturn(Optional.of(me));
        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());
        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());

        // [수정된 Repository 메서드 호출] 삭제된 댓글도 포함하는 메서드 Mocking
        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));

        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
                .willReturn(Collections.emptyList());

        given(s3UrlUtil.toFullUrl(anyString())).willReturn("http://full-url.com/profile.jpg");

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);
        assertThat(dto.getContent()).isEqualTo("정상 댓글입니다.");
        assertThat(dto.getUserName()).isEqualTo("작성자");
        assertThat(dto.getUserId()).isEqualTo(writerId);
        assertThat(dto.getUserProfileUrl()).isEqualTo("http://full-url.com/profile.jpg");
    }

    @Test
    @DisplayName("마스킹: 차단된 사용자(내가 차단함 + 나를 차단함)")
    void getComments_BlockedUser() {
        // given
        Long verificationId = 100L;
        Long myId = 1L;
        Long blockedUserId = 99L;

        User me = createUser(myId, "나", UserStatus.ACTIVE);
        User blockedUser = createUser(blockedUserId, "차단남", UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId);

        Comment comment = createComment(11L, verification, blockedUser, "차단된 유저의 글", false, false, null);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(myId)).willReturn(Optional.of(me));

        // 차단 목록 설정
        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(List.of(blockedUserId));
        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());

        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));

        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
                .willReturn(Collections.emptyList());

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);
        assertThat(dto.getContent()).isEqualTo("차단된 사용자의 댓글입니다.");
        assertThat(dto.getUserName()).isEqualTo("(알 수 없음)");
        assertThat(dto.getUserId()).isNull();
        assertThat(dto.getUserProfileUrl()).isNull();
    }

    @Test
    @DisplayName("마스킹: 탈퇴한 사용자 (Inactive Status)")
    void getComments_InactiveUser() {
        // given
        Long verificationId = 100L;
        Long myId = 1L;

        User me = createUser(myId, "나", UserStatus.ACTIVE);
        User inactiveUser = createUser(50L, "탈퇴자", UserStatus.INACTIVE);
        Verification verification = createVerification(verificationId);

        Comment comment = createComment(12L, verification, inactiveUser, "탈퇴 전 남긴 글", false, false, null);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(myId)).willReturn(Optional.of(me));
        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());
        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());

        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));

        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
                .willReturn(Collections.emptyList());

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);
        assertThat(dto.getContent()).isEqualTo("탈퇴한 사용자의 댓글입니다.");
        assertThat(dto.getUserName()).isEqualTo("(알 수 없음)");
        assertThat(dto.getUserId()).isNull();
    }

    @Test
    @DisplayName("마스킹: 삭제된 댓글 (Soft Delete) - Repository에서 조회되어 마스킹 처리됨")
    void getComments_Deleted() {
        // given
        Long verificationId = 100L;
        Long myId = 1L;

        User me = createUser(myId, "나", UserStatus.ACTIVE);
        User writer = createUser(2L, "작성자", UserStatus.ACTIVE);
        Verification verification = createVerification(verificationId);

        // isDeleted = true 로 생성
        Comment comment = createComment(13L, verification, writer, "삭제되어 안 보이는 글", true, false, null);

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
        given(userRepository.findById(myId)).willReturn(Optional.of(me));
        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());
        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());

        // [중요] 삭제된 댓글도 반환하도록 Mocking
        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));

        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
                .willReturn(Collections.emptyList());

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);

        // Converter 로직 검증
        assertThat(dto.getContent()).isEqualTo("삭제된 댓글입니다.");
        assertThat(dto.getUserName()).isEqualTo("삭제");
        assertThat(dto.getUserId()).isNull();
    }

    @Test
    @DisplayName("익명: 타인의 익명 댓글은 닉네임과 ID가 가려져야 함")
    void getComments_Anonymous_Other() {
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
        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());

        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(comment)));

        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
                .willReturn(Collections.emptyList());

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);
        assertThat(dto.getContent()).isEqualTo("비밀글입니다");
        assertThat(dto.getUserName()).isEqualTo("익명3"); // 마스킹 닉네임
        assertThat(dto.getUserId()).isNull(); // ID 숨김
        assertThat(dto.isAnonymous()).isTrue();
    }
}