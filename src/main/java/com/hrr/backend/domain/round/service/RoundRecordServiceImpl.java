package com.hrr.backend.domain.round.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.report.repository.WeakVerificationReportRepository;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.verification.repository.VerificationAbsenceLogRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoundRecordServiceImpl implements RoundRecordService {

	private final RoundRecordRepository roundRecordRepository;

	private final WeakVerificationReportRepository weakVerificationReportRepository;

	private final VerificationAbsenceLogRepository verificationAbsenceLogRepository;

	private final UserChallengeRepository userChallengeRepository;

	private final ChallengeRepository challengeRepository;

	@Override
	@Transactional
	public void synchronizeWarnCount(Long roundRecordId) {
		// 라운드 기록 조회
		RoundRecord roundRecord = roundRecordRepository.findByIdWithPessimisticLock(roundRecordId)
			.orElseThrow(() -> new GlobalException(ErrorCode.ROUND_RECORD_NOT_FOUND));

		// 부실 인증 신고 수와 미인증 로그 수 조회
		long weakReportCount = weakVerificationReportRepository.countByRoundRecordId(roundRecordId);
		long absenceCount = verificationAbsenceLogRepository.countByRoundRecordId(roundRecordId);

		// 경고 횟수 계산: (부실 신고 / 3) + 미인증 횟수
		int calculatedWarnCount = (int) (weakReportCount / 3) + (int) absenceCount;

		// 경고 횟수 동기화 (퇴출 기능 폐지로 KICKED 자동 전환은 더 이상 발생하지 않음)
        roundRecord.synchronizeWarnCount(calculatedWarnCount);
	}

	// 퇴출 처리 진행
	private void processKickOutSideEffects(Long challengeId) {
		Challenge targetChallenge = challengeRepository.findById(challengeId)
			.orElseThrow(() -> new GlobalException(ErrorCode.CHALLENGE_NOT_FOUND));

		// -- 챌린지 인원 재계산
		// 실제 JOINED 상태인 인원만 다시 카운트
		int actualCount = (int) userChallengeRepository.countByChallengeIdAndStatus(
			targetChallenge.getId(), ChallengeJoinStatus.JOINED);

		// 챌린지 엔티티의 인원수 필드를 실제 값으로 덮어쓰기
		targetChallenge.updateCurrentParticipants(actualCount);

		// --필요 시 추가 작업 진행(알림 발송 등)
	}
}
