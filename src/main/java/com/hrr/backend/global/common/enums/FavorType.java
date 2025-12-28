package com.hrr.backend.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FavorType {
	GENDER("성별"),
	AGE_GROUP("연령대"),
	JOB("직업"),
	AVAILABLE_TIME("이용 가능 시간"),
	CATEGORY("카테고리"),
	GOAL("목표");

	private final String description;
}
