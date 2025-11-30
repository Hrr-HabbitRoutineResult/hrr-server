package com.hrr.backend.domain.comment.repository;

import com.hrr.backend.domain.comment.entity.Comment;
import com.hrr.backend.domain.verification.entity.Verification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** 특정 인증글의 댓글 조회 (부모 댓글만 depth = 0) */
    List<Comment> findByVerificationAndDepthOrderByCreatedAtAsc(Verification verification, int depth);

    /** 특정 부모댓글의 대댓글 조회 */
    List<Comment> findByParentOrderByCreatedAtAsc(Comment parent);

    /** 인증글 삭제 대비 전체 댓글 조회 (관리자용) */
    List<Comment> findByVerification(Verification verification);
}
