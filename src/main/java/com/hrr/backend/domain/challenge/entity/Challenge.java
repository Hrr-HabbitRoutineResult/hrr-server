package com.hrr.backend.domain.challenge.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.hrr.backend.domain.round.entity.Round;
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

	// 비즈니스 규칙
	public static final int ROUND_WEEKS = 3;              // 챌린지 1라운드 길이 (3주)
	public static final int OWNER_DECISION_DAYS = 5;      // 방장 연장 결정 (종료 5일 전)
	public static final int CHALLENGER_DECISION_DAYS = 3; // 팀원 연장 결정 (종료 3일 전)
	public static final int FINALIZATION_DAYS = 1;        // 최종 확정 및 신규 모집 (종료 1일 전)

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 현재 진행 중인 라운드
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "current_round_id")
	private Round currentRound;

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
	private ChallengeStatus status = ChallengeStatus.UPCOMING;

	@Column(name = "image_key")
	private String imageKey;

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

	// 참가자 수 변경
	public void updateCurrentParticipants(int currentParticipants) {
		this.currentParticipants = currentParticipants;
	}

	// 참가자 수 증가 편의 메서드
	public void increaseCurrentParticipants() {
		this.currentParticipants++;
	}

	// 참가자 수 감소 편의 메서드
	public void decreaseCurrentParticipants() {
		if (this.currentParticipants > 0) {
			this.currentParticipants--;
		}
	}

	// 라운드 교체
	public void changeCurrentRound(Round round) {
		this.currentRound = round;
	}

    //상태 변경 메서드 추가
    public void updateStatus(ChallengeStatus status) {
        this.status = status;
    }

}
