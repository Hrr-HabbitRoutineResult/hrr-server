package com.hrr.backend.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.report.entity.WeakVerificationReport;

public interface WeakVerificationReportRepository extends JpaRepository<WeakVerificationReport, Long> {
}
