package com.hrr.backend.domain.verification.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.verification.entity.VerificationAbsenceLog;

import java.time.LocalDate;

public interface VerificationAbsenceLogRepository extends JpaRepository<VerificationAbsenceLog, Long> {

	// 특정 라운드의 특정 챌린저의 미인증 횟수 조회
	long countByRoundRecordId(Long roundRecordId);

    // 특정 챌린저의 특정 기간(주차 범위) 내 미인증 횟수 조회
    long countByRoundRecordIdAndAbsenceDateBetween(Long roundRecordId, LocalDate start, LocalDate end);
}
