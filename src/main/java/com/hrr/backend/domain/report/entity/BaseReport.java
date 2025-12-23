package com.hrr.backend.domain.report.entity;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.common.BaseEntity;
import com.hrr.backend.global.common.enums.ReportReason;
import com.hrr.backend.global.common.enums.ReportStatus;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseReport extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reporter_id", nullable = false)
	private User reporter; // 신고자

	@Enumerated(EnumType.STRING)
	private ReportReason reason; // 선택 사유

	private String description;	// 기타(직접 입력) 사유

	@Enumerated(EnumType.STRING)
	private ReportStatus status = ReportStatus.PENDING; // 신고 처리 상태

	// 자식의 빌더가 호출할 생성자
	protected BaseReport(User reporter, ReportReason reason, String description) {
		this.reporter = reporter;
		this.reason = reason;
		this.description = description;
	}
}
