package com.hrr.backend.domain.comment.converter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.hrr.backend.domain.comment.entity.enums.CommentMaskingType;
import org.springframework.stereotype.Component;

import com.hrr.backend.domain.comment.dto.CommentResponseDto;
import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.global.s3.S3UrlUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CommentConverter {

    private final S3UrlUtil s3UrlUtil;

    /**
     * Comment 객체 받아서 응답 형태로 변환
     * @param comment 변환할 댓글 객체
     * @param currentUserId 조회하는 사용자의 userId; 마스킹 처리 여부를 판단하는 데 사용
     * @param blockedUserIds 차단된 사용자 ID 목록 (내가 차단한 + 나를 차단한 사용자)
     * @return
     */
    public CommentResponseDto toDto(Comment comment, Long currentUserId, Set<Long> blockedUserIds) {
        /**
         * reponse에 필요한 항목들 계산 - 1. comment에서 그대로 가져오는 값들
         */
        Long commentId = comment.getId();
        Long verificationId = comment.getVerification().getId();	// 어떤 인증글(게시글)에 달린 댓글인지
        User commentOwner = comment.getUser();	// 댓글 작성자

        boolean isAnonymous = comment.isAnonymous();
        int depth = comment.getDepth();
        boolean isAdopted = comment.isAdopted();
        int likesCount = comment.getLikesCount();

        LocalDateTime createdAt = comment.getCreatedAt();
        LocalDateTime updatedAt = comment.getUpdatedAt();

        /**
         * reponse에 필요한 항목들 계산 - 2. 추가 계산 필요한 값들
         */
        // 부모 댓글 id
        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;

        // 내 댓글인지 여부
        boolean isMyComment = commentOwner.getId().equals(currentUserId);

        // 마스킹 조건 확인
        boolean isOwnerDeleted = comment.isDeleted(); //댓글 삭제
        boolean isOwnerInactive = commentOwner.isNotActive(); // 사용자 탈퇴
        boolean isOwnerBlocked = blockedUserIds != null && blockedUserIds.contains(commentOwner.getId()); //내가 차단함

        CommentMaskingType maskingType = CommentMaskingType.NONE;
        if (isOwnerDeleted) {
            maskingType = CommentMaskingType.DELETED;
        } else if (isOwnerBlocked) {
            maskingType = CommentMaskingType.BLOCKED;
        } else if (isOwnerInactive) {
            maskingType = CommentMaskingType.INACTIVE;
        }

        // 마스킹이 필요한 경우 (삭제 or 탈퇴 or 차단)
        boolean needsMasking = isOwnerDeleted || isOwnerInactive || isOwnerBlocked;

        // 프로필 이미지
        String userProfileUrl = null;
        if (!needsMasking) {
            if (isAnonymous) {
                // 익명 댓글: 마스킹된 기본 프로필 이미지 사용
                userProfileUrl = null; // 또는 기본 익명 프로필 URL
            } else {
                // 실명 댓글: 실제 프로필 이미지
                userProfileUrl = s3UrlUtil.toFullUrl(commentOwner.getProfileImage());
            }
        }

        // 댓글 작성자가 해당 인증글(게시글)의 작성자인지 여부
        boolean isCommentFromVerificationAuthor = Optional.ofNullable(comment.getVerification())
                .map(verification -> verification.getUserChallenge())
                .map(userChallenge -> userChallenge.getUser())
                .map(User::getId)
                .map(id -> id.equals(commentOwner.getId()))
                .orElse(false);

        // 닉네임 결정 (우선순위: 삭제 > 차단 > 탈퇴 > 익명 > 실명)
        String userName;

        switch (maskingType) {
            case DELETED:
                userName = "삭제";
                break;
            case BLOCKED:
                userName = null;  // 차단된 경우 null
                break;
            case INACTIVE:
                userName = "탈퇴한 사용자";
                break;
            default:  // NONE
                // 기본 이름 설정 (익명 vs 실명)
                if (isAnonymous) {
                    userName = isCommentFromVerificationAuthor
                            ? "익명(글쓴이)"
                            : "익명" + Objects.toString(comment.getAnonymousNumber(), "");
                } else {
                    userName = commentOwner.getDisplayNickname();
                    if (isCommentFromVerificationAuthor) {
                        userName += "(글쓴이)";
                    }
                }
        }

        // userId - 마스킹이 필요하거나 익명이면서 본인 댓글이 아닌 경우 null
        Long userId = null;
        if (!needsMasking) {
            // 익명이면서 조회하는 사람의 댓글이 아니면 null
            boolean maskingCondition = isAnonymous && !isMyComment;
            userId = maskingCondition ? null : commentOwner.getId();
        }

        // 내용
        String content;
        switch (maskingType) {
            case DELETED:
                content = "삭제된 댓글입니다.";
                break;
            case BLOCKED:
                content = "차단된 사용자의 댓글입니다.";
                break;
            case INACTIVE:
            case NONE:
            default:
                // 탈퇴한 사용자와 일반 사용자는 원본 내용 표시
                content = comment.getContent();
        }


        return CommentResponseDto.builder()
                .commentId(commentId)
                .parentId(parentId)
                .verificationId(verificationId)
                // 작성자 ID
                .userId(userId)
                // 닉네임 반환
                .userName(userName)
                // 프로필 이미지 URL
                .userProfileUrl(userProfileUrl)
                .isMyComment(isMyComment)
                .isAnonymous(isAnonymous)
                .depth(depth)
                .content(content)
                .likesCount(likesCount)
                .isAdopted(isAdopted)
                .maskingType(maskingType)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}