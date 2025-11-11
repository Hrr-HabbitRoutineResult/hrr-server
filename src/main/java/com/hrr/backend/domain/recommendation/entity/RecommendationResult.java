package com.hrr.backend.domain.recommendation.entity;

import com.hrr.backend.global.common.BaseEntity;
import com.hrr.backend.domain.user.entity.UserFavor;
import com.hrr.backend.domain.challenge.entity.Challenge;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "favor_id", nullable = false)
    private UserFavor userFavor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @Column(name = "cosine_score")
    private Float cosineScore;

    @Column(name = "ranking", nullable = false)
    private Integer ranking;

}
