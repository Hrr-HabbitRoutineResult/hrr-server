package com.hrr.backend.domain.recommendation.entity;

import com.hrr.backend.global.common.BaseEntity;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserFavor;
import com.hrr.backend.domain.challenge.entity.Challenge;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recommendation_result")
@Builder
public class RecommendationResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // user와 1:1
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // user_favor와 N:1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favor_id", nullable = false)
    private UserFavor userFavor;

    // challenge와 N:1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "cosine_score")
    private Float cosineScore;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
