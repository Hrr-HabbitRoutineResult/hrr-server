package com.hrr.backend.domain.report.entity;

import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "weak_verification_report",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_reporter_verification",
			columnNames = {"reporter_id", "verification_id"}
		)
	}
)
public class WeakVerificationReport extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id", nullable = false)
	private User reporter;	// 신고자

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "verification_id", nullable = false)
	private Verification verification;	// 신고 대상 인증

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "round_record_id", nullable = false)
	private RoundRecord roundRecord; // 페널티 대상자의 라운드 기록

}
