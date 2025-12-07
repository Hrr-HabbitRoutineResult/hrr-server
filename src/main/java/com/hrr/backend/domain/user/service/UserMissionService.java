package com.hrr.backend.domain.user.service;

import java.time.LocalDate;

import com.hrr.backend.domain.user.dto.UserMissionResponseDto;
import com.hrr.backend.domain.user.entity.User;

public interface UserMissionService {

	// 오늘의 랜덤미션 조회
	UserMissionResponseDto.DetailDto getRandomMission(User user);

	// 오늘의 랜덤미션 완료 여부 조회
	Boolean	getRandomMissionStatus(User user);

	// 오늘의 랜덤미션 인증
	void verifyRandomMission(User user, Long missionId, LocalDate date, String imageKey);
}
