package com.hrr.backend.domain.challenge.entity;

import java.time.LocalDateTime;

import com.hrr.backend.global.common.BaseEntity;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;

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
	private LocalDateTime verifyStartTime;

	@Column(name = "verify_end_time", nullable = false)
	private LocalDateTime verifyEndTime;

	@Lob // Text 타입 매핑
	@Column(name = "rule", columnDefinition = "TEXT")
	private String rule;

	@Column(name = "current_participants", nullable = false)
	private Integer currentParticipants;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false)
	private ChallengeStatus status=ChallengeStatus.UPCOMING;

	@Column(name = "imageUrl")
	private String imageUrl;
}
