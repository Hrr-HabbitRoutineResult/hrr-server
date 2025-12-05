package com.hrr.backend.domain.comment.entity;

import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verification_id", nullable = false)
    private Verification verification;

    @Column(nullable = false)
    private String content;

    // 채택 여부
    @Column(nullable = false)
    @ColumnDefault("false")
    private Boolean isAdopted = false;

    public void adopt() {
        this.isAdopted = true;
    }
}