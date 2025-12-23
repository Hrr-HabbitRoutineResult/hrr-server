package com.hrr.backend.domain.report.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.report.dto.ReportRequestDto;
import com.hrr.backend.domain.report.entity.VerificationPostReport;
import com.hrr.backend.domain.report.repository.VerificationPostReportRepository;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

	private final VerificationRepository verificationRepository;
	private final VerificationPostReportRepository verificationPostReportRepository;

	@Override
	@Transactional
	public void reportVerificationPost(Long reporterId, ReportRequestDto request) {
		// 신고 대상 조회
		Verification verification = verificationRepository.findById(request.getTargetId())
			.orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

		// 신고 내역 저장
		VerificationPostReport report = VerificationPostReport.builder()
			.reporterId(reporterId)
			.verification(verification)
			.reason(request.getReason())
			.description(request.getDescription())
			.build();
		verificationPostReportRepository.save(report);

		// 비즈니스 로직 처리 (5회 누적 시 차단)
		verification.addReport(); // 엔티티 내부에서 count++ 및 상태 변경 로직 수행

		verificationRepository.save(verification);
	}
}
