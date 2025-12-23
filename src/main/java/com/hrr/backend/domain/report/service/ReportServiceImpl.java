package com.hrr.backend.domain.report.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.report.dto.ReportRequestDto;
import com.hrr.backend.domain.report.entity.UserReport;
import com.hrr.backend.domain.report.entity.VerificationPostReport;
import com.hrr.backend.domain.report.repository.UserReportRepository;
import com.hrr.backend.domain.report.repository.VerificationPostReportRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
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

	private final UserRepository userRepository;
	private final UserReportRepository userReportRepository;

	@Override
	@Transactional
	public void reportVerificationPost(User reporter, ReportRequestDto request) {
		// 신고 대상 조회
		Verification verification = verificationRepository.findById(request.getTargetId())
			.orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

		// 차단 확인 (가장 먼저!)
		// 이미 차단된 글이라면 다른 검증을 할 필요도 없이 바로 예외를 던집니다.
		if (VerificationStatus.BLOCKED.equals(verification.getStatus())) {
			throw new GlobalException(ErrorCode.ACCESS_DENIED_REPORTED_POST);
		}

		// 자기 신고 방지
		if (verification.getUserChallenge().getUser().getId().equals(reporter.getId())) {
			throw new GlobalException(ErrorCode.CANNOT_REPORT_OWN_POST);
		}

		// 중복 신고 방지
		if (verificationPostReportRepository.existsByReporterAndVerification(reporter, verification)) {
			throw new GlobalException(ErrorCode.ALREADY_REPORTED);
		}

		// 신고 내역 저장
		VerificationPostReport report = VerificationPostReport.builder()
			.reporter(reporter)
			.verification(verification)
			.reason(request.getReason())
			.description(request.getDescription())
			.build();
		verificationPostReportRepository.save(report);

		// 비즈니스 로직 처리 (5회 누적 시 차단)
		verification.addReport(); // 엔티티 내부에서 count++ 및 상태 변경 로직 수행

		verificationRepository.save(verification);
	}

	@Override
	@Transactional
	public void reportUser(User reporter, ReportRequestDto request) {
		// 유저 신고는 기록만 남김
		User targetUser = userRepository.findById(request.getTargetId())
			.orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

		// 중복 신고 방지
		if (userReportRepository.existsByReporterAndTargetUser(reporter, targetUser)) {
			throw new GlobalException(ErrorCode.ALREADY_REPORTED_USER);
		}

		UserReport report = UserReport.builder()
			.reporter(reporter)
			.targetUser(targetUser)
			.reason(request.getReason())
			.description(request.getDescription())
			.build();

		userReportRepository.save(report);
	}
}
