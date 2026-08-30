package com.hrr.backend.global.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.service.RoundRecordService;
import com.hrr.backend.domain.verification.repository.VerificationAbsenceLogRepository;
import com.hrr.backend.domain.verification.service.VerificationAbsenceService;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.logging.SafeExceptionSummary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class VerificationScheduler {

	private final RoundRecordRepository roundRecordRepository;

	private final VerificationAbsenceService verificationAbsenceService;


	@Scheduled(cron = "0 5 0 * * *") // 매일 00 : 05 실행
	@Transactional
	// 어제가 인증 요일이었지만 인증 기록이 없는, 즉 미인증 기록을 로그로 남기기 위한 스케줄러
	// 효율성을 위해 전체 데이터 확인이 아닌 어제 하루만의 미인증을 매일 기록
	public void checkAbsence() {
		// 어제 날짜
		LocalDate yesterdayDate = LocalDate.now().minusDays(1);

		// Java 요일을 챌린지 요일 Enum으로 변환
		ChallengeDays yesterdayChallengeDay = ChallengeDays.from(yesterdayDate.getDayOfWeek());

		// 미인증 대상자 조회
		List<RoundRecord> absentees = roundRecordRepository.findAbsentees(yesterdayChallengeDay, yesterdayDate);
		int failCount = 0;

		for (RoundRecord record : absentees) {
			try {
				verificationAbsenceService.processAbsentee(record, yesterdayDate); // 인증이 완료되지 않은 요일은 체크 대상인 어제이므로 어제를 미인증날짜로 기록
			} catch (Exception e) {
				log.warn("[checkAbsence] 미인증 처리에 실패했습니다. roundRecordId={}, failure={}",
					record.getId(), SafeExceptionSummary.summarize(e));
				failCount++;
			}
		}
		if (failCount > 0) {
			log.error("[checkAbsence] 미인증 처리 실패가 누적되었습니다. targetCount={}, failureCount={}",
				absentees.size(), failCount);
		}
		log.info("[checkAbsence] 미인증 처리를 완료했습니다. targetCount={}, failedCount={}", absentees.size(), failCount);
	}
}
