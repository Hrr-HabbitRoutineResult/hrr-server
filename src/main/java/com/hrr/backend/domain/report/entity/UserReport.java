package com.hrr.backend.domain.report.entity;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.common.enums.ReportReason;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
	name = "user_report",
	indexes = {
		@Index(name = "idx_user_report_reporter_id", columnList = "reporter_id"),
		@Index(name = "idx_user_report_target_user_id", columnList = "target_user_id")
	}
)
public class UserReport extends BaseReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id", nullable = false)
    private User targetUser;	// 신고 당하는 유저

	@Builder
	public UserReport(User reporter, ReportReason reason, String description, User targetUser) {
		// BaseReport의 생성자를 호출하여 부모 필드 채움
		super(reporter, reason, description);
		this.targetUser = targetUser;
	}
}
