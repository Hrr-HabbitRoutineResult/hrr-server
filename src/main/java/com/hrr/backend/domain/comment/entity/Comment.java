package com.hrr.backend.domain.comment.entity;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "comment",
        indexes = {
                @Index(name = "idx_comment_verification", columnList = "verification_id"),
                @Index(name = "idx_comment_parent", columnList = "parent_id")
        }
)
@SQLDelete(sql = "UPDATE comment SET is_deleted = true WHERE id = ?")
@Where(clause = "is_deleted = false")
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 인증글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id", nullable = false)
    private Verification verification;

    /** 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 내용 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 부모 댓글 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    /** 깊이 (댓글=0, 대댓글=1) */
    @Column(nullable = false)
    private int depth;

    /** 익명 여부 */
    @Column(nullable = false)
    private boolean isAnonymous;

    /** 좋아요 수 */
    @Column(nullable = false)
    private int likesCount;

    /** Soft Delete */
    @Column(nullable = false)
    private boolean isDeleted;

    // === 정적 팩토리 메서드 ===
    public static Comment create(
            Verification verification,
            User user,
            Comment parent,
            String content,
            boolean isAnonymous,
            int depth
    ) {
        Comment comment = new Comment();
        comment.verification = verification;
        comment.user = user;
        comment.parent = parent;
        comment.content = content;
        comment.isAnonymous = isAnonymous;
        comment.depth = depth;
        comment.likesCount = 0;
        comment.isDeleted = false;
        return comment;
    }

    // === 내용 수정 ===
    public void updateContent(String content) {
        this.content = content;
    }

    // === Soft Delete 처리 (Hibernate @SQLDelete 와 동일하게 isDeleted만 조작)
    public void softDelete() {
        this.isDeleted = true;
    }
}

