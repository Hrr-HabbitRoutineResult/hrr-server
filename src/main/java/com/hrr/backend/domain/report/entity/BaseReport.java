package com.hrr.backend.domain.report.entity;

import com.hrr.backend.global.common.BaseEntity;
import com.hrr.backend.global.common.enums.ReportReason;
import com.hrr.backend.global.common.enums.ReportStatus;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseReport extends BaseEntity {

	private Long reporterId; // 신고자

	@Enumerated(EnumType.STRING)
	private ReportReason reason; // 선택 사유

	private String description;	// 기타(직접 입력) 사요

	@Enumerated(EnumType.STRING)
	private ReportStatus status = ReportStatus.PENDING; // 신고 처리 상태

	// 자식의 빌더가 호출할 생성자
	protected BaseReport(Long reporterId, ReportReason reason, String description) {
		this.reporterId = reporterId;
		this.reason = reason;
		this.description = description;
	}
}
