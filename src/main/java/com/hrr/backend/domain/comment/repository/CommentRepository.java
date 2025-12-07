package com.hrr.backend.domain.comment.repository;

import com.hrr.backend.domain.comment.entity.Comment;
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

    /** * 특정 부모댓글의 대댓글 조회
     * 삭제되지 않은 댓글만
     */
    List<Comment> findByParentAndIsDeletedFalseOrderByCreatedAtAsc(Comment parent);

    @Query("SELECT c FROM Comment c WHERE c.verification.id = :verificationId AND c.isAdopted = true")
    List<Comment> findAdoptedCommentsByVerificationId(@Param("verificationId") Long verificationId);


    /**
     * 여러 부모 댓글에 대한 대댓글을 한 번에 조회 (N+1 방지용)
     */
    List<Comment> findByParentInAndIsDeletedFalseOrderByCreatedAtAsc(List<Comment> parents);

    Optional<Comment> findByIdAndIsDeletedFalse(Long id);
}
