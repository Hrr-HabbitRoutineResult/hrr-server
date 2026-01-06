package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeStatics;
import com.hrr.backend.domain.recommendation.dto.response.ChallengeItemDto;
import com.hrr.backend.global.common.enums.FavorType;

import java.util.List;

public interface ChallengeStaticsService {

	/**
	 * 특정 챌린지의 모든 선호 타입에 대한 기본 통계 데이터를 생성합니다.
	 */
	void createInitialStatics(Challenge challenge);

	/**
	 * 특정 챌린지의 통계 데이터를 찾아 수치를 증가시킵니다.
	 */
	void updateChallengeStatics(Challenge challenge);

	/**
	 * 특정 선호 타입과 값에 대한 통계를 조회하고, 없으면 초기 생성 후 해당 데이터를 반환합니다.
	 */
	ChallengeStatics getOrUpdateStatics(Challenge challenge, FavorType type, String value);

    void applyStaticsToItems(List<ChallengeItemDto> allChallenges);
}
