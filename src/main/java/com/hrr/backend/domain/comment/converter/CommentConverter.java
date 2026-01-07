package com.hrr.backend.domain.comment.converter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
        boolean isOwnerDeleted = comment.isDeleted();
        boolean isOwnerInactive = commentOwner.isNotActive();
        boolean isOwnerBlocked = blockedUserIds != null && blockedUserIds.contains(commentOwner.getId());

        // 마스킹이 필요한 경우 (삭제 or 탈퇴 or 차단)
        boolean needsMasking = isOwnerDeleted || isOwnerInactive || isOwnerBlocked;

        // 프로필 이미지
        String userProfileUrl = null;
        if (!needsMasking && !isAnonymous) {
            // 소셜 플랫폼의 프로필: 이미 full URL이라 그대로 반환
            // 직접 업로드한 이미지: AWS S3의 image Key만 저장되기에 prefix 추가 필요
            userProfileUrl = s3UrlUtil.toFullUrl(commentOwner.getProfileImage());
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

        if (isOwnerDeleted) {
            userName = "삭제";
        } else if (isOwnerBlocked) {
            // 차단된 사용자 마스킹
            userName = "(알 수 없음)";
        } else if (isOwnerInactive) {
            // 탈퇴한 사용자 마스킹
            userName = "(알 수 없음)";
        } else {
            // 기본 이름 설정 (익명 vs 실명)
            if (isAnonymous) {
                // 익명 댓글의 경우, 본인 댓글이어도 익명 번호 표시
                userName = isCommentFromVerificationAuthor ? "익명(글쓴이)" : "익명" + Objects.toString(comment.getAnonymousNumber(), "");
            } else {
                // 실명
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
        if (isOwnerDeleted) {
            content = "삭제된 댓글입니다.";
        } else if (isOwnerBlocked) {
            content = "차단된 사용자의 댓글입니다.";
        } else if (isOwnerInactive) {
            content = "탈퇴한 사용자의 댓글입니다.";
        } else {
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
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}