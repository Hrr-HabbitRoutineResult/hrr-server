package com.hrr.backend.domain.verification.service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.service.RoundRecordService;
import com.hrr.backend.domain.verification.entity.VerificationAbsenceLog;
import com.hrr.backend.domain.verification.repository.VerificationAbsenceLogRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VerificationAbsenceService {
	private final VerificationAbsenceLogRepository absenceLogRepository;
	private final RoundRecordService roundRecordService;

	@Transactional(propagation = Propagation.REQUIRES_NEW) // 각 건마다 독립적인 트랜잭션 보장
	public void processAbsentee(RoundRecord record, LocalDate date) {
		// 미인증 로그 저장
		absenceLogRepository.save(VerificationAbsenceLog.builder()
			.roundRecord(record)
			.absenceDate(date)
			.build());

		// 경고 횟수를 업데이트 후 퇴출 여부를 결정하는 메소드 호출
		roundRecordService.synchronizeWarnCount(record.getId());
	}
}
