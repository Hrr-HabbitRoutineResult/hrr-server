package com.hrr.backend.domain.comment.service;

import com.hrr.backend.domain.comment.converter.CommentConverter;
import com.hrr.backend.domain.comment.dto.CommentCreateRequestDto;
import com.hrr.backend.domain.comment.dto.CommentListResponseDto;
import com.hrr.backend.domain.comment.dto.CommentResponseDto;
import com.hrr.backend.domain.comment.dto.CommentUpdateRequestDto;
import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.comment.repository.CommentRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserBlockRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final VerificationRepository verificationRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommentConverter commentConverter;
    private final UserBlockRepository userBlockRepository;

    /** 댓글 작성 */
    @Override
    @Transactional
    public CommentResponseDto createComment(Long verificationId, Long userId, CommentCreateRequestDto requestDto) {

        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        Comment parent = null;

        // parentId 가 있으면 대댓글
        if (requestDto.getParentId() != null) {
            parent = commentRepository.findByIdAndIsDeletedFalse(requestDto.getParentId())
                    .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_INVALID_PARENT));

            //부모 댓글의 dpth가 1이상 존재해서 대댓글인 경우에 더 이상 답글을 달 수 없도록 진행
            if (parent.getDepth() >= 1) {
                throw new GlobalException(ErrorCode.COMMENT_DEPTH_EXCEEDED);
            }

            // 부모 댓글의 인증글과 현재 인증글이 다르면 에러
            if (!parent.getVerification().getId().equals(verificationId)) {
                throw new GlobalException(ErrorCode.COMMENT_INVALID_PARENT);
            }
        }

        Integer anonymousNumber = null;

        if (requestDto.isAnonymous()) {
            // 해당 인증글에서 이 유저가 이미 쓴 익명 댓글이 있는지 확인
            Optional<Comment> existingComment = commentRepository
                    .findFirstByVerificationAndUserAndIsAnonymousTrue(verification, user);

            if (existingComment.isPresent()) {
                // 있다면 해당 익명 번호 그대로 사용
                anonymousNumber = existingComment.get().getAnonymousNumber();
            } else {
                // 없다면 해당 인증글의 최대 익명 번호 조회 후 +1
                Integer maxNumber = commentRepository.findMaxAnonymousNumberByVerification(verification);
                anonymousNumber = (maxNumber == null) ? 1 : maxNumber + 1;
            }
        }

        Comment comment = Comment.create(
                verification,
                user,
                parent,
                requestDto.getContent(),
                requestDto.isAnonymous(),
                anonymousNumber,
                parent == null ? 0 : parent.getDepth() + 1
        );

        commentRepository.save(comment);

        // 차단 관계 조회 (댓글 작성 시에는 자신의 댓글이므로 빈 Set 전달)
        Set<Long> blockedUserIds = Collections.emptySet();

        return commentConverter.toDto(comment, userId, blockedUserIds);
    }

    /** 댓글/대댓글 조회 */
    @Override
    public CommentListResponseDto getComments(Long verificationId, Long userId, Pageable pageable) {

        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

        // 조회하는 사용자 정보 가져오기
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

        // 차단 관계 조회 (내가 차단한 + 나를 차단한 사용자)
        Set<Long> blockedUserIds = getBlockedUserIds(currentUser);

        // 부모 댓글 조회 시 삭제된 댓글도 포함 (IsDeletedFalse 제거된 메서드 사용)
        Page<Comment> parentPage = commentRepository
                .findByVerificationAndDepthOrderByCreatedAtAsc(verification, 0, pageable);
        List<Comment> parents = new ArrayList<>(parentPage.getContent());

        Optional<Comment> adoptedOpt =
                commentRepository.findAdoptedCommentsByVerificationId(verificationId);

        Comment adoptedParent = null;
        List<CommentResponseDto> adoptedChildren = new ArrayList<>();

        if (adoptedOpt.isPresent()) {
            Comment adopted = adoptedOpt.get();

            adoptedParent = (adopted.getDepth() == 0)
                    ? adopted
                    : adopted.getParent();

            if (adoptedParent != null) {
                // 채택된 댓글의 대댓글들도 삭제된 내역 포함하여 조회
                adoptedChildren = commentRepository
                        .findByParentOrderByCreatedAtAsc(adoptedParent)
                        .stream()
                        .map(child -> commentConverter.toDto(child, userId, blockedUserIds))
                        .toList();


                // 중복 방지: 만약 현재 페이지 부모 리스트에 들어있다면 제거
                Long adoptedParentId = adoptedParent.getId();
                parents.removeIf(p -> p.getId().equals(adoptedParentId));
            }
        }

        List<CommentResponseDto> result = new ArrayList<>();


        if (!parents.isEmpty()) {
            // 부모 댓글들에 달린 대댓글들을 한 번에 조회할 때도 삭제된 댓글 포함
            List<Comment> children = commentRepository
                    .findByParentInOrderByCreatedAtAsc(parents);
            // parentId 기준으로 자식 댓글들을 그룹핑
            Map<Long, List<Comment>> childrenMap = children.stream()
                    .collect(Collectors.groupingBy(
                            c -> c.getParent().getId(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            for (Comment parent : parents) {
                // 부모 댓글 추가
                result.add(commentConverter.toDto(parent, userId, blockedUserIds));

                // 부모에 해당하는 자식(대댓글)들 추가
                List<Comment> childList = childrenMap.getOrDefault(parent.getId(), Collections.emptyList());
                for (Comment child : childList) {
                    result.add(commentConverter.toDto(child, userId, blockedUserIds));
                }
            }
        }

        return CommentListResponseDto.builder()
                .adoptedParent(adoptedParent != null ? commentConverter.toDto(adoptedParent, userId, blockedUserIds) : null)
                .adoptedChildren(adoptedChildren)
                .comments(result)
                .currentPage(parentPage.getNumber() + 1)
                .totalPages(parentPage.getTotalPages())
                .totalParentElements(parentPage.getTotalElements())
                .size(parentPage.getSize())
                .isFirst(parentPage.isFirst())
                .isLast(parentPage.isLast())
                .build();
    }

    /** 댓글 수정 */
    @Override
    @Transactional
    public CommentResponseDto updateComment(Long commentId, Long userId, CommentUpdateRequestDto requestDto) {

        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            throw new GlobalException(ErrorCode.COMMENT_NOT_FOUND);
        }

        if (!comment.getUser().getId().equals(userId)) {

            throw new GlobalException(ErrorCode.COMMENT_UNAUTHORIZED);
        }

        comment.updateContent(requestDto.getContent());

        // 수동으로 DB 에 반영하여 Auditing 필드(updatedAt)를 갱신
        commentRepository.saveAndFlush(comment);

        // 자신의 댓글이므로 차단 관계는 빈 Set
        Set<Long> blockedUserIds = Collections.emptySet();

        return commentConverter.toDto(comment, userId, blockedUserIds);
    }


    /** 댓글 삭제 (Soft Delete) */
    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {

        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.COMMENT_UNAUTHORIZED);
        }

        // 엔티티에 @SQLDelete가 적용되어 있으므로 repository.delete를 호출하면 update 쿼리가 실행
        commentRepository.delete(comment);
    }

    /**
     * 차단 관계 조회 헬퍼 메서드
     * @param currentUser 조회하는 사용자
     * @return 차단된 사용자 ID Set (내가 차단한 + 나를 차단한 사용자)
     */
    private Set<Long> getBlockedUserIds(User currentUser) {
        Long userId = currentUser.getId();
        Set<Long> blockedIds = new HashSet<>();

        // 1. 내가 차단한 유저 ID 목록
        blockedIds.addAll(userBlockRepository.findBlockedIdsByBlockerId(userId));

        // 2. 나를 차단한 유저 ID 목록 (양방향 차단 적용)
        blockedIds.addAll(userBlockRepository.findBlockerIdsByBlockedId(userId));

        return blockedIds;
    }
}