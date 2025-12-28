package com.hrr.backend.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.report.entity.VerificationPostReport;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.Verification;

public interface VerificationPostReportRepository extends JpaRepository<VerificationPostReport,Long> {

	// 특정 사용자가 특정 인증 게시글에 대해 신고한 내역이 있는지 확인
	boolean existsByReporterAndVerification(User reporter, Verification verification);
}
