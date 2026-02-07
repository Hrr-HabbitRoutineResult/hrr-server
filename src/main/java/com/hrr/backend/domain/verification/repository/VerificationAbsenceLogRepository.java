package com.hrr.backend.domain.verification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.verification.entity.VerificationAbsenceLog;

public interface VerificationAbsenceLogRepository extends JpaRepository<VerificationAbsenceLog, Long> {

	// 특정 라운드의 특정 챌린저의 미인증 횟수 조회
	long countByRoundRecordId(Long roundRecordId);
}
