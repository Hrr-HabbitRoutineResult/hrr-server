package com.hrr.backend.domain.comment.converter;

import java.time.LocalDateTime;
import java.util.Optional;

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
	 * @return
	 */
    public CommentResponseDto toDto(Comment comment, Long currentUserId) {
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

		// 프로필 이미지 - 소셜 플랫폼에서 그대로 가져오는 경우와 직접 업로드 한 이미지인 경우 분리
		// 소셜 플랫폼의 프로필: 이미 full URL이라 그대로 반환
		// 직접 업로드한 이미지: AWS S3의 image Key만 저장되기에 prefix 추가 필요
		// 익명이면 null 반환
		String userProfileUrl = !isAnonymous? s3UrlUtil.toFullUrl(commentOwner.getProfileImage()) : null ;	//util에서 자동 처리

		// 닉네임 - 닉네임 or 마스킹 or (알 수 없음)
		// 마스킹 조건 - 익명이면서, 조회하는 사람의 댓글이 아님
		boolean isMyComment = commentOwner.getId().equals(currentUserId);

		boolean maskingCondition = isAnonymous && !isMyComment;

		String userName;

		// 댓글 작성자가 해당 인증글(게시글)의 작성자인지 여부
		boolean isCommentFromVerificationAuthor = Optional.ofNullable(comment.getVerification())
			    .map(verification -> verification.getUserChallenge())
			    .map(userChallenge -> userChallenge.getUser())
			    .map(User::getId)
			    .map(id -> id.equals(commentOwner.getId()))
			    .orElse(false);

		// 우선순위: 삭제>탈퇴>익명>실명
		if (comment.isDeleted()) {
			userName = "삭제";
		} else if (commentOwner.getUserStatus() == UserStatus.DELETED) {
			userName = "알 수 없음";
		} else {
			// 기본 이름 설정 (익명 vs 실명)
			if (isAnonymous) {
				// 익명
				userName = isCommentFromVerificationAuthor ? "익명(글쓴이)" : "익명" + comment.getAnonymousNumber();
			} else {
				// 실명
				userName = commentOwner.getNickname();
				if (isCommentFromVerificationAuthor) {
					userName += "(글쓴이)";
				}
			}
		}

		// userId
		Long userId = (maskingCondition)? null : commentOwner.getId();	// 마스킹 조건이면 null, 아니면 작성자의 userId

		// 내용
		String content = comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();


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
