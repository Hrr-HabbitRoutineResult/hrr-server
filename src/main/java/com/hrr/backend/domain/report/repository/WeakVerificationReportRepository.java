package com.hrr.backend.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.report.entity.WeakVerificationReport;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.verification.entity.Verification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WeakVerificationReportRepository extends JpaRepository<WeakVerificationReport, Long> {

	// 특정 라운드의 특정 챌린저의 부실인증 개수 조회
	long countByRoundRecordId(Long roundRecordId);

	// 중복 체크 - 특정 사용자가 특정 인증을 신고한 내역이 있는지 조회
	boolean existsByReporterAndVerification(User reporter, Verification targetVerification);
}
