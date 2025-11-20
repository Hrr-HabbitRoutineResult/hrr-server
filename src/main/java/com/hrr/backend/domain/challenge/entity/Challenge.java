package com.hrr.backend.domain.challenge.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.hrr.backend.global.common.BaseEntity;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import com.hrr.backend.domain.recommendation.entity.RecommendationResult;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "challenge")
@Builder
public class Challenge extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Column(name = "is_public", nullable = false)
	private Boolean isPublic;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false)
	private Category category;

	@Column(name = "is_viewer_mode", nullable = false)
	private Boolean isViewerMode;

	@Column(name = "max_participants", nullable = false)
	private Integer maxParticipants;	// 최대 30명

	@Column(name = "password")
	private String password;	// 4자리

	@Column(name = "title", nullable = false)
	private String title;

	@Column(name = "description", nullable = false)
	private String description;

	@Column(name = "start_date", nullable = false)
	private LocalDateTime startDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "verification_method", nullable = false)
	private VerificationType verificationType;

	@Column(name = "verify_start_time", nullable = false)
	private LocalTime verifyStartTime;

	@Column(name = "verify_end_time", nullable = false)
	private LocalTime verifyEndTime;

	@Lob // Text 타입 매핑
	@Column(name = "rule", columnDefinition = "TEXT")
	private String rule;

	@Column(name = "current_participants", nullable = false)
	private Integer currentParticipants;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@Builder.Default
	private ChallengeStatus status=ChallengeStatus.UPCOMING;

	@Column(name = "image_url")
	private String imageUrl;

	@Column(name = "like_count")
	private Integer likeCount;	// 좋아요 수; 집계용

	@OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<ChallengeDayJoin> challengeDays = new ArrayList<>();

    @OneToOne(mappedBy = "challenge", cascade = CascadeType.ALL)
    private ChallengeEmbedding embedding;

    @Builder.Default
    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<RecommendationResult> recommendationResults = new ArrayList<>();

	/**
	 * Increments the challenge's current participant count by one.
	 *
	 * <p>Does not validate or enforce the configured maximum participant limit.
	 */
	public void increaseCurrentParticipants() {
		this.currentParticipants++;
	}

}