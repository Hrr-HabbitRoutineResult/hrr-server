package com.hrr.backend.global.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrr.backend.domain.user.service.UserDeleteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserScheduler {
	private final UserDeleteService userDeleteService;

	// 매일 새벽 4시에 실행
	@Scheduled(cron = "0 5 1 * * *")
	public void cleanupOldDeletedUsers() {
		// 현재 시간으로부터 한 달 전 시점 계산
		LocalDateTime threshold = LocalDateTime.now().minusMonths(1);

		log.info("한 달 경과 탈퇴 회원 삭제 시작: 기준 시점 {}", threshold);

		try {
			userDeleteService.executeHardDelete(threshold);
			log.info("탈퇴 회원 정리가 완료되었습니다.");
		} catch (Exception e) {
			log.error("탈퇴 회원 정리 중 오류 발생: ", e);
		}
	}
}
