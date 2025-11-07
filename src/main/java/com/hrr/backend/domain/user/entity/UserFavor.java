package com.hrr.backend.domain.user.entity;

import com.hrr.backend.global.common.BaseEntity;
import com.hrr.backend.domain.recommendation.entity.RecommendationResult;
import com.hrr.backend.global.common.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_favor")
public class UserFavor extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "job", nullable = false)
    private Job job;

    // 다중 선택 가능
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_favor_available_time",
            joinColumns = @JoinColumn(name = "user_favor_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "available_time", nullable = false)
    private Set<AvailableTime> availableTime = new LinkedHashSet<>();

    // 다중 선택 가능
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_favor_category",
            joinColumns = @JoinColumn(name = "user_favor_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Set<Category> category = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "goal", nullable = false)
    private Goal goal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "userFavor", cascade = CascadeType.ALL)
    private UserFavorEmbedding userFavorEmbedding;

    @OneToMany(mappedBy = "userFavor", cascade = CascadeType.ALL, orphanRemoval = false)
    private java.util.List<RecommendationResult> recommendationResults = new java.util.ArrayList<>();


}
