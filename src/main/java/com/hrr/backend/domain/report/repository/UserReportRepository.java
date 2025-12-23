package com.hrr.backend.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.report.entity.UserReport;

public interface UserReportRepository extends JpaRepository<UserReport, Long> {
}
