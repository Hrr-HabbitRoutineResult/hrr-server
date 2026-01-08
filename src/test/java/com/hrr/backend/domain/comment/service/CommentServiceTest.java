//package com.hrr.backend.domain.comment.service;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.*;
//
//import java.time.LocalDateTime;
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
//import org.springframework.test.util.ReflectionTestUtils; // 핵심 import
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
//    private CommentServiceImpl commentService;
//    private CommentConverter commentConverter;
//
//    @Mock private VerificationRepository verificationRepository;
//    @Mock private CommentRepository commentRepository;
//    @Mock private UserRepository userRepository;
//    @Mock private UserBlockRepository userBlockRepository;
//    @Mock private S3UrlUtil s3UrlUtil;
//
//    @BeforeEach
//    void setUp() {
//        // Converter는 실제 로직 동작 확인을 위해 직접 생성
//        commentConverter = new CommentConverter(s3UrlUtil);
//
//        // Service 생성 (생성자 주입)
//        commentService = new CommentServiceImpl(
//                verificationRepository,
//                commentRepository,
//                userRepository,
//                commentConverter,
//                userBlockRepository
//        );
//    }
//
//    // --- 헬퍼 메서드 ---
//
//    private User createUser(Long id, String nickname, UserStatus status) {
//        return User.builder()
//                .id(id)
//                .nickname(nickname)
//                .userStatus(status)
//                .profileImage("profile.jpg")
//                .build();
//    }
//
//    private Verification createVerification(Long id) {
//        return Verification.builder()
//                .id(id)
//                .build();
//    }
//
//    /**
//     * [핵심 해결] Reflection을 사용하여 Comment 객체 생성 및 createdAt 주입
//     */
//    private Comment createComment(Long id, Verification verification, User user, String content, boolean isDeleted, boolean isAnonymous, Integer anonymousNumber) {
//        Comment comment = Comment.builder()
//                .id(id)
//                .verification(verification)
//                .user(user)
//                .content(content)
//                .isDeleted(isDeleted)
//                .isAnonymous(isAnonymous)
//                .anonymousNumber(anonymousNumber)
//                .likesCount(0)
//                .depth(0)
//                // .createdAt(LocalDateTime.now())  <-- 여기서 에러 나던 것을 제거
//                .build();
//
//        // 부모 클래스(BaseEntity)의 필드는 Reflection으로 주입
//        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.now());
//
//        return comment;
//    }
//
//    // --- 테스트 케이스 ---
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
//        Verification verification = createVerification(verificationId);
//
//        // 헬퍼 메서드로 Comment 생성 (Reflection 적용됨)
//        Comment comment = createComment(10L, verification, writer, "정상 댓글입니다.", false, false, null);
//
//        // Mocking
//        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
//        given(userRepository.findById(myId)).willReturn(Optional.of(me));
//        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());
//        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());
//
//        // [수정된 Repository 메서드 호출] 삭제된 댓글도 포함하는 메서드 Mocking
//        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
//                .willReturn(new PageImpl<>(List.of(comment)));
//
//        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
//                .willReturn(Collections.emptyList());
//
//        given(s3UrlUtil.toFullUrl(anyString())).willReturn("http://full-url.com/profile.jpg");
//
//        // when
//        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));
//
//        // then
//        CommentResponseDto dto = result.getComments().get(0);
//        assertThat(dto.getContent()).isEqualTo("정상 댓글입니다.");
//        assertThat(dto.getUserName()).isEqualTo("작성자");
//        assertThat(dto.getUserId()).isEqualTo(writerId);
//        assertThat(dto.getUserProfileUrl()).isEqualTo("http://full-url.com/profile.jpg");
//    }
//
//    @Test
//    @DisplayName("마스킹: 차단된 사용자(내가 차단함 + 나를 차단함)")
//    void getComments_BlockedUser() {
//        // given
//        Long verificationId = 100L;
//        Long myId = 1L;
//        Long blockedUserId = 99L;
//
//        User me = createUser(myId, "나", UserStatus.ACTIVE);
//        User blockedUser = createUser(blockedUserId, "차단남", UserStatus.ACTIVE);
//        Verification verification = createVerification(verificationId);
//
//        Comment comment = createComment(11L, verification, blockedUser, "차단된 유저의 글", false, false, null);
//
//        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
//        given(userRepository.findById(myId)).willReturn(Optional.of(me));
//
//        // 차단 목록 설정
//        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(List.of(blockedUserId));
//        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());
//
//        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
//                .willReturn(new PageImpl<>(List.of(comment)));
//
//        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
//                .willReturn(Collections.emptyList());
//
//        // when
//        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));
//
//        // then
//        CommentResponseDto dto = result.getComments().get(0);
//        assertThat(dto.getContent()).isEqualTo("차단된 사용자의 댓글입니다.");
//        assertThat(dto.getUserName()).isEqualTo("(알 수 없음)");
//        assertThat(dto.getUserId()).isNull();
//        assertThat(dto.getUserProfileUrl()).isNull();
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
//        User inactiveUser = createUser(50L, "탈퇴자", UserStatus.INACTIVE);
//        Verification verification = createVerification(verificationId);
//
//        Comment comment = createComment(12L, verification, inactiveUser, "탈퇴 전 남긴 글", false, false, null);
//
//        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
//        given(userRepository.findById(myId)).willReturn(Optional.of(me));
//        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());
//        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());
//
//        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
//                .willReturn(new PageImpl<>(List.of(comment)));
//
//        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
//                .willReturn(Collections.emptyList());
//
//        // when
//        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));
//
//        // then
//        CommentResponseDto dto = result.getComments().get(0);
//        assertThat(dto.getContent()).isEqualTo("탈퇴한 사용자의 댓글입니다.");
//        assertThat(dto.getUserName()).isEqualTo("(알 수 없음)");
//        assertThat(dto.getUserId()).isNull();
//    }
//
//    @Test
//    @DisplayName("마스킹: 삭제된 댓글 (Soft Delete) - Repository에서 조회되어 마스킹 처리됨")
//    void getComments_Deleted() {
//        // given
//        Long verificationId = 100L;
//        Long myId = 1L;
//
//        User me = createUser(myId, "나", UserStatus.ACTIVE);
//        User writer = createUser(2L, "작성자", UserStatus.ACTIVE);
//        Verification verification = createVerification(verificationId);
//
//        // isDeleted = true 로 생성
//        Comment comment = createComment(13L, verification, writer, "삭제되어 안 보이는 글", true, false, null);
//
//        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
//        given(userRepository.findById(myId)).willReturn(Optional.of(me));
//        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());
//        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());
//
//        // [중요] 삭제된 댓글도 반환하도록 Mocking
//        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
//                .willReturn(new PageImpl<>(List.of(comment)));
//
//        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
//                .willReturn(Collections.emptyList());
//
//        // when
//        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));
//
//        // then
//        CommentResponseDto dto = result.getComments().get(0);
//
//        // Converter 로직 검증
//        assertThat(dto.getContent()).isEqualTo("삭제된 댓글입니다.");
//        assertThat(dto.getUserName()).isEqualTo("삭제");
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
//        Verification verification = createVerification(verificationId);
//
//        // 익명, anonymousNumber=3
//        Comment comment = createComment(14L, verification, other, "비밀글입니다", false, true, 3);
//
//        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(verification));
//        given(userRepository.findById(myId)).willReturn(Optional.of(me));
//        given(userBlockRepository.findBlockedIdsByBlockerId(myId)).willReturn(Collections.emptyList());
//        given(userBlockRepository.findBlockerIdsByBlockedId(myId)).willReturn(Collections.emptyList());
//
//        given(commentRepository.findByVerificationAndDepthOrderByCreatedAtAsc(any(), anyInt(), any(Pageable.class)))
//                .willReturn(new PageImpl<>(List.of(comment)));
//
//        given(commentRepository.findByParentInOrderByCreatedAtAsc(anyList()))
//                .willReturn(Collections.emptyList());
//
//        // when
//        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));
//
//        // then
//        CommentResponseDto dto = result.getComments().get(0);
//        assertThat(dto.getContent()).isEqualTo("비밀글입니다");
//        assertThat(dto.getUserName()).isEqualTo("익명3"); // 마스킹 닉네임
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

    // --- 테스트 케이스 ---

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

        // when
        CommentListResponseDto result = commentService.getComments(verificationId, myId, PageRequest.of(0, 10));

        // then
        CommentResponseDto dto = result.getComments().get(0);
        assertThat(dto.getContent()).isEqualTo("정상 댓글입니다.");
        assertThat(dto.getUserName()).isEqualTo("작성자");
        assertThat(dto.getMaskingType()).isEqualTo(CommentMaskingType.NONE);
        assertThat(dto.getUserProfileUrl()).isEqualTo("http://full-url.com/profile.jpg");
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
}