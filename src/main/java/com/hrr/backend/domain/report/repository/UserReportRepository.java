package com.hrr.backend.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.report.entity.UserReport;
import com.hrr.backend.domain.user.entity.User;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {

	// 특정 신고자가 특정 사용자를 대상으로 한 신고가 있는지 확인
	boolean existsByReporterAndTargetUser(User reporter, User targetUser);
}
