package com.hrr.backend.global.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.user.service.UserDeleteService;

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

		log.info("한 달 경과 탈퇴 회원 삭제 시작: 기준 시점 {}", threshold);

		// 상태가 DELETED 이고, deletedAt 이 30일 이전인 사용자 조회
		List<User> usersToClean = userRepository
			.findUserToDelete(threshold);

		for (User user : usersToClean) {
			try {
				userDeleteService.processPermanentWithdrawal(user);
			} catch (Exception e) {
				// 특정 사용자 처리 실패 시 로그를 남기고 다음 사용자 진행
				log.error("탈퇴 회원 정리 중 오류 발생: {}", user.getId(), e);
			}
		}
		log.info("탈퇴 회원 정리가 완료되었습니다.");
	}
}
