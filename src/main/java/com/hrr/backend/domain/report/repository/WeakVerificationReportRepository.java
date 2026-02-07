package com.hrr.backend.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.report.entity.WeakVerificationReport;

public interface WeakVerificationReportRepository extends JpaRepository<WeakVerificationReport, Long> {

	// 특정 라운드의 특정 챌린저의 부실인증 개수 조회
	long countByRoundRecordId(Long roundRecordId);
}
