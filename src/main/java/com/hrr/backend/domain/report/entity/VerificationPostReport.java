package com.hrr.backend.domain.report.entity;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.common.enums.ReportReason;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
	name = "verification_post_report",
	indexes = {
		@Index(name = "idx_v_report_reporter_id", columnList = "reporter_id"),
		@Index(name = "idx_v_report_verification_id", columnList = "verification_id")
	}
)
public class VerificationPostReport extends BaseReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "verification_id")
	private Verification verification; // 신고 대상 게시글

	@Builder
	public VerificationPostReport(User reporter, ReportReason reason, String description, Verification verification) {
		// BaseReport의 생성자를 호출하여 부모 필드 채움
		super(reporter, reason, description);
		this.verification = verification;
	}
}
