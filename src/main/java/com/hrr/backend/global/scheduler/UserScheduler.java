package com.hrr.backend.global.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.user.service.UserDeleteService;
import com.hrr.backend.global.logging.SafeExceptionSummary;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserScheduler {
	private final UserDeleteService userDeleteService;
	private final UserRepository userRepository;

	// 매일 새벽 4시에 실행
	@Scheduled(cron = "0 0 4 * * *")
	public void cleanupOldDeletedUsers() {
		// 현재 시간으로부터 한 달 전 시점 계산
		LocalDateTime threshold = LocalDateTime.now().minusMonths(1);

		// 상태가 INACTIVE 이고, deletedAt 이 30일 이전인 사용자 조회
		List<Long> userIdsToClean = userRepository
			.findUserIdsToDelete(threshold);
		int failCount = 0;
		for (Long userId : userIdsToClean) {
			try {
				userDeleteService.processPermanentWithdrawal(userId);
			} catch (Exception e) {
				// 특정 사용자 처리 실패 시 로그를 남기고 다음 사용자 진행
				log.warn("[cleanupOldDeletedUsers] 탈퇴 회원 정리에 실패했습니다. userId={}, failure={}",
					userId, SafeExceptionSummary.summarize(e));
				failCount++;
			}
		}
		if (failCount > 0) {
			log.error("[cleanupOldDeletedUsers] 탈퇴 회원 정리에 실패가 누적되었습니다. targetCount={}, failureCount={}",
				userIdsToClean.size(), failCount);
		}
		log.info("[cleanupOldDeletedUsers] 탈퇴 회원 정리를 완료했습니다. targetCount={}, failedCount={}",
			userIdsToClean.size(), failCount);
	}
}
