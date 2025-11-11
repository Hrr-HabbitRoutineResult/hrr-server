package com.hrr.backend.domain.user.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.hrr.backend.domain.user.dto.UserMissionResponseDto;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserMission;
import com.hrr.backend.domain.user.repository.UserMissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserMissionServiceImpl implements UserMissionService {

	private final UserMissionRepository userMissionRepository;

	@Override
	public UserMissionResponseDto.DetailDto getRandomMission(Long userId) {
		return null;
	}

	@Override
	public Boolean getRandomMissionStatus(User user) {
		LocalDate today = LocalDate.now();

		UserMission userMission = userMissionRepository.findByUserAndDate(user, today).orElse(null);

		if (userMission == null || !userMission.getIsCompleted()) {
			// 오늘자 랜덤미션이 아직 생성되지 않았거나 완료되지 않았을 때, false
			return false;
		}

		return true;
	}
}
