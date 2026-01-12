package com.hrr.backend.domain.comment.repository;

import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.Verification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 인증글의 부모 댓글(Depth 0) 조회
     * 삭제되지 않은 댓글만
     */
    Page<Comment> findByVerificationAndDepthAndIsDeletedFalseOrderByCreatedAtAsc(
            Verification verification,
            int depth,
            Pageable pageable
    );

    // 부모 댓글 조회 (삭제된 댓글 포함) - 댓글 목록 조회용
    Page<Comment> findByVerificationAndDepthOrderByCreatedAtAsc(
            Verification verification,
            int depth,
            Pageable pageable
    );

    /** * 특정 부모댓글의 대댓글 조회
     * 삭제되지 않은 댓글만
     */
    List<Comment> findByParentAndIsDeletedFalseOrderByCreatedAtAsc(Comment parent);

    // 대댓글 조회 (삭제된 댓글 포함) - 채택된 댓글의 대댓글 조회용 등
    List<Comment> findByParentOrderByCreatedAtAsc(Comment parent);

    @Query("SELECT c FROM Comment c WHERE c.verification.id = :verificationId AND c.isAdopted = true")
    Optional<Comment> findAdoptedCommentsByVerificationId(@Param("verificationId") Long verificationId);


    /**
     * 여러 부모 댓글에 대한 대댓글을 한 번에 조회 (N+1 방지용)
     */
    List<Comment> findByParentInAndIsDeletedFalseOrderByCreatedAtAsc(List<Comment> parents);

    // 여러 부모 댓글의 대댓글 조회 (삭제된 댓글 포함) - N+1 방지용
    List<Comment> findByParentInOrderByCreatedAtAsc(List<Comment> parents);

    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

	/**
	 * 특정 인증글에 특정 유저가 익명으로 남긴 댓글이 있는지 조회
	 */
	Optional<Comment> findFirstByVerificationAndUserAndIsAnonymousTrue(Verification verification, User user);

	/**
	 * 특정 인증글에 달린 익명 댓글 중 최대 익명 번호 조회 (익명1, 익명2, 익명3 있으면 3 반환)
	 */
	@Query("SELECT MAX(c.anonymousNumber) FROM Comment c WHERE c.verification = :verification")
	Integer findMaxAnonymousNumberByVerification(Verification verification);

    long countByVerification(Verification verification);
}
