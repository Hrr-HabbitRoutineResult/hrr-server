package com.hrr.backend.domain.report.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.report.dto.ReportRequestDto;
import com.hrr.backend.domain.report.entity.UserReport;
import com.hrr.backend.domain.report.entity.VerificationPostReport;
import com.hrr.backend.domain.report.entity.WeakVerificationReport;
import com.hrr.backend.domain.report.repository.UserReportRepository;
import com.hrr.backend.domain.report.repository.VerificationPostReportRepository;
import com.hrr.backend.domain.report.repository.WeakVerificationReportRepository;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.service.RoundRecordService;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
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

	private final UserChallengeRepository userChallengeRepository;

	private final WeakVerificationReportRepository weakVerificationReportRepository;

	private final RoundRecordService roundRecordService;

	@Override
	@Transactional
	public void reportWeakVerification(User reporter,Long verificationId) {
		// 신고 대상 조회(Verification)
		Verification targetVerification = verificationRepository.findByIdWithPessimisticLock(verificationId)
			.orElseThrow(() -> new GlobalException(ErrorCode.VERIFICATION_NOT_FOUND));

		// 피신고자 정보 조회(RoundRecord)
		RoundRecord targetRecord = targetVerification.getRoundRecord();

		// 권한 검증: 신고자와 피신고자가 동일 챌린지에 참여 중인지 확인 - 검증 완료 시 다음 단계로 이동
		validateChallengeParticipation(reporter, targetVerification.getUserChallenge().getChallenge());

		// 신고 내역 저장
		WeakVerificationReport report = WeakVerificationReport.builder()
			.reporter(reporter)
			.verification(targetVerification)
			.roundRecord(targetRecord)
			.build();
		weakVerificationReportRepository.save(report);

		// 경고 횟수를 업데이트 후 퇴출 여부를 결정하는 메소드 호출
		roundRecordService.synchronizeWarnCount(targetRecord.getId());
	}

	/**
	 * 신고자와 피신고자가 같은 챌린지에 참여하고 있는지를 검증
	 * @param reporter 신고자 User 객체
	 * @param challenge 확인하려는 챌린지
	 */
	private void validateChallengeParticipation(User reporter, Challenge challenge) {
		if (!userChallengeRepository.existsByUserAndChallenge(reporter, challenge)) {
			throw new GlobalException(ErrorCode.CANNOT_REPORT_OTHER_CHALLENGE_VERIFICATION);
		}
	}

	@Override
	@Transactional
	public void reportVerificationPost(User reporter, ReportRequestDto request) {
		// 신고 대상 조회
		Verification verification = verificationRepository.findByIdWithPessimisticLock(request.getTargetId())
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

		// 자기 자신 신고 검증
		if (reporter.getId().equals(targetUser.getId())) {
			throw new GlobalException(ErrorCode.CANNOT_REPORT_SELF);
		}

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
