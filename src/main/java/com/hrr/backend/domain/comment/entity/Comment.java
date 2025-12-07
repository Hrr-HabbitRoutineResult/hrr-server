package com.hrr.backend.domain.comment.entity;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
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

    /** 작성자 (develop 추가) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 내용 (병합: TEXT 타입 + null 불가) */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 부모 댓글 (develop 추가) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    /** 깊이 (댓글=0, 대댓글=1) (develop 추가) */
    @Column(nullable = false)
    private int depth;

    /** 익명 여부 (develop 추가) */
    @Column(nullable = false)
    private boolean isAnonymous;

    /** 좋아요 수 (develop 추가) */
    @Column(nullable = false)
    private int likesCount;

    /** Soft Delete (develop 추가) */
    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    /** 채택 여부 (feat/90 추가) */
    @Column(nullable = false)
    @ColumnDefault("false")
    @Builder.Default
    private Boolean isAdopted = false;

    // === 정적 팩토리 메서드 (develop) ===
    public static Comment create(
            Verification verification,
            User user,
            Comment parent,
            String content,
            boolean isAnonymous,
            int depth
    ) {
        return Comment.builder()
                .verification(verification)
                .user(user)
                .parent(parent)
                .content(content)
                .isAnonymous(isAnonymous)
                .depth(depth)
                .likesCount(0)
                .isDeleted(false)
                .isAdopted(false) // 초기값 설정
                .build();
    }

    // === 내용 수정 (develop) ===
    public void updateContent(String content) {
        this.content = content;
    }

    // === Soft Delete 처리 (develop) ===
    public void softDelete() {
        this.isDeleted = true;
    }

    // === 채택 처리 (feat/90) ===
    public void adopt() {
        this.isAdopted = true;
    }
}