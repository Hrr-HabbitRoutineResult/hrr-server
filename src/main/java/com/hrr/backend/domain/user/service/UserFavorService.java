package com.hrr.backend.domain.user.service;

import java.util.LinkedHashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.recommendation.dto.request.ChallengeRecommendRequest;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserFavor;
import com.hrr.backend.domain.user.repository.UserFavorRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
// 선호 관련 작업이 많지 않아 인터페이스 생략
public class UserFavorService {
	private final UserFavorRepository userFavorRepository;
	private final UserRepository userRepository;

	@Transactional
	public void saveUserFavor(ChallengeRecommendRequest request) {
		// 해당 유저가 존재하는지 확인
		User user = userRepository.findById(request.getUserId())
			.orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND));

		// 2. DTO 데이터를 엔티티로 변환 (빌더 패턴 활용)
		UserFavor userFavor = UserFavor.builder()
			.user(user)
			.gender(request.getGender())
			.ageGroup(request.getAgeGroup())
			.job(request.getJob())
			// List를 LinkedHashSet으로 변환하여 순서 보장 및 중복 제거
			.availableTime(new LinkedHashSet<>(request.getAvailableTime()))
			.category(new LinkedHashSet<>(request.getCategory()))
			.goal(request.getGoal())
			.build();

		// 저장
		userFavorRepository.save(userFavor);
	}
}
