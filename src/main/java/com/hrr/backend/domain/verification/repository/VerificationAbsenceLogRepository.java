package com.hrr.backend.domain.verification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.verification.entity.VerificationAbsenceLog;

public interface VerificationAbsenceLogRepository extends JpaRepository<VerificationAbsenceLog, Long> {
}
