package com.hrr.backend.global.scheduler;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.service.RoundRecordService;
import com.hrr.backend.domain.verification.entity.VerificationAbsenceLog;
import com.hrr.backend.domain.verification.repository.VerificationAbsenceLogRepository;
import com.hrr.backend.global.common.enums.ChallengeDays;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VerificationScheduler {

	private final RoundRecordRepository roundRecordRepository;

	private final VerificationAbsenceLogRepository verificationAbsenceLogRepository;

	private final RoundRecordService roundRecordService;

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

		for (RoundRecord record : absentees) {
			// 미인증 로그 저장
			verificationAbsenceLogRepository.save(VerificationAbsenceLog.builder()
				.roundRecord(record)
				.absenceDate(yesterdayDate)	// 인증이 완료되지 않은 요일은 체크 대상인 어제이므로 어제를 미인증날짜로 기록
				.build());

			// 경고 횟수를 업데이트 후 퇴출 여부를 결정하는 메소드 호출
			roundRecordService.synchronizeWarnCount(record.getId());
		}
	}
}
