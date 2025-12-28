package com.hrr.backend.domain.challenge.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeStatics;
import com.hrr.backend.domain.challenge.repository.ChallengeStaticsRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.UserFavor;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.global.common.enums.AgeGroup;
import com.hrr.backend.global.common.enums.AvailableTime;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.FavorType;
import com.hrr.backend.global.common.enums.Gender;
import com.hrr.backend.global.common.enums.Goal;
import com.hrr.backend.global.common.enums.Job;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChallengeStaticsServiceImpl implements ChallengeStaticsService {
	private final ChallengeStaticsRepository staticsRepository;
	private final UserChallengeRepository userChallengeRepository;

	/**
	 * 특정 챌린지의 모든 선호 타입에 대한 기본 통계 데이터를 생성합니다.
	 */
	@Transactional
	@Override
	public void createInitialStatics(Challenge challenge) {
		// 저장할 통계 데이터 모음
		List<ChallengeStatics> initialStaticsList = new ArrayList<>();

		// FavorType의 모든 항목을 가져옴 (GENDER, AGE_GROUP, JOB 등)
		for (FavorType type : FavorType.values()) {

			// 각 타입에 해당하는 상세 값 리스트를 가져와서 각각 생성
			// 예: GENDER 타입이면 [MALE, FEMALE] 리스트를 가져옴
			List<String> values = getValuesForType(type);

			for (String value : values) {
				initialStaticsList.add(ChallengeStatics.builder()
					.challenge(challenge)
					.favorType(type)
					.favorValue(value)
					.number(0)	// 초기값 0으로 설정
					.build());
			}
		}

		// 한 번의 쿼리로 대량 저장
		staticsRepository.saveAll(initialStaticsList);
	}

	/**
	 * 특정 챌린지의 통계 데이터의 수치를 업데이트 합니다.
	 */
	@Transactional
	@Override
	public void updateChallengeStatics(Challenge challenge) {
		// 해당 챌린지의 모든 참여 정보 조회(JOINED인 것만)
		List<UserChallenge> participations = userChallengeRepository.findAllJoinedWithUserFavorByChallengeId(challenge.getId());

		if (participations.isEmpty()) return;

		// 집계를 위한 임시 맵 (FavorType별로 상세 값과 카운트를 저장)
		Map<FavorType, Map<String, Integer>> aggregateMap = new HashMap<>();

		for (UserChallenge pc : participations) {
			User user = pc.getUser();
			List<UserFavor> userFavors = user.getUserFavors(); // List로 가져오기

			if (userFavors == null || userFavors.isEmpty()) continue;

			// 한 유저가 가진 모든 선호 정보 세트를 순회
			for (UserFavor favor : userFavors) {
				if (favor == null) continue;

				// 단일 선택 항목들 집계
				if (favor.getGender() != null) {
					accumulate(aggregateMap, FavorType.GENDER, favor.getGender().name());
				}
				if (favor.getAgeGroup() != null) {
					accumulate(aggregateMap, FavorType.AGE_GROUP, favor.getAgeGroup().name());
				}
				if (favor.getJob() != null) {
					accumulate(aggregateMap, FavorType.JOB, favor.getJob().name());
				}
				if (favor.getGoal() != null) {
					accumulate(aggregateMap, FavorType.GOAL, favor.getGoal().name());
				}

				// 다중 선택 항목 (AvailableTime) 집계
				if (favor.getAvailableTime() != null) {
					favor.getAvailableTime().forEach(time ->
						accumulate(aggregateMap, FavorType.AVAILABLE_TIME, time.name()));
				}

				// 다중 선택 항목 (Category) 집계
				if (favor.getCategory() != null) {
					favor.getCategory().forEach(cat ->
						accumulate(aggregateMap, FavorType.CATEGORY, cat.name()));
				}
			}
		}

		// 집계된 데이터를 ChallengeStatics 테이블에 최종 반영
		applyAggregatedData(challenge, aggregateMap);
	}

	/**
	 * 특정 선호 타입과 그 종류에 대한 통계를 조회하고 없으면 기본 테이블 생성 후 해당하는 데이터만 반환
	 * @param challenge 조회 챌린지
	 * @param type 선호 타입
	 * @param value 선호 종류
	 * @return 해당하는 통계 row
	 */
	@Transactional
	@Override
	public ChallengeStatics getOrUpdateStatics(Challenge challenge, FavorType type, String value) {
		// 먼저 조회 시도
		return staticsRepository.findByChallengeAndFavorTypeAndFavorValue(challenge, type, value)
			.orElseGet(() -> {
				// 데이터가 없으면 이 챌린지에 대한 모든 통계 기본 레코드를 생성
				createInitialStatics(challenge);

				// 생성 후, 요청했던 특정 조건의 데이터를 다시 찾아 반환
				return staticsRepository.findByChallengeAndFavorTypeAndFavorValue(challenge, type, value)
					.orElseThrow(() -> new GlobalException(ErrorCode.STATICS_NOT_FOUND));
			});
	}

	/**
	 * 선호 타입에 해당하는 옵션들을 불러오는 메서드
	 * @param type 선호 타입(GENDER, JOB 등)
	 * @return
	 */
	private List<String> getValuesForType(FavorType type) {
		return switch (type) {
			case GENDER -> Arrays.stream(Gender.values()).map(Enum::name).toList();
			case AGE_GROUP -> Arrays.stream(AgeGroup.values()).map(Enum::name).toList();
			case JOB -> Arrays.stream(Job.values()).map(Enum::name).toList();
			case AVAILABLE_TIME -> Arrays.stream(AvailableTime.values()).map(Enum::name).toList();
			case CATEGORY -> Arrays.stream(Category.values()).map(Enum::name).toList();
			case GOAL -> Arrays.stream(Goal.values()).map(Enum::name).toList();
			default -> Collections.emptyList();
		};
	}

	// 맵에 숫자를 누적시키는 헬퍼 메서드
	private void accumulate(Map<FavorType, Map<String, Integer>> counts, FavorType type, String value) {
		counts.computeIfAbsent(type, k -> new HashMap<>())
			.merge(value, 1, Integer::sum);
	}

	// 최종적으로 DB에 저장하는 메서드
	private void applyAggregatedData(Challenge challenge, Map<FavorType, Map<String, Integer>> aggregateMap) {
		// 해당 챌린지의 모든 기존 통계 데이터를 가져와서 수치를 0으로 초기화
		List<ChallengeStatics> allStatics = staticsRepository.findByChallenge(challenge);

		// 최초 참여 시 getOrUpdateStatics 내부에서 createInitialStatics가 실행되므로 여기서는 기존 데이터가 있을 때만 0으로 초기화
		allStatics.forEach(s -> s.updateNumber(0));

		// 집계된 결과 반영
		aggregateMap.forEach((type, valueMap) -> {
			valueMap.forEach((value, count) -> {
				ChallengeStatics statics = getOrUpdateStatics(challenge, type, value);
				statics.updateNumber(count);	// 합산된 결과로 업데이트
			});
		});
	}
}
