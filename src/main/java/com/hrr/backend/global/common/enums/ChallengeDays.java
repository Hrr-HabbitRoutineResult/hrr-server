package com.hrr.backend.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;

@Getter
@RequiredArgsConstructor
public enum ChallengeDays {

	// DayCode를 JavaScript의 Date.getDay()와 일치시킴 (일요일 = 0)

	SUNDAY("일", 0),
	MONDAY("월", 1),
	TUESDAY("화", 2),
	WEDNESDAY("수", 3),
	THURSDAY("목", 4),
	FRIDAY("금", 5),
	SATURDAY("토", 6);

	private final String koreanName;
	private final int dayCode;       // 일요일=0 시작

	public static ChallengeDays from(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MONDAY -> MONDAY;
			case TUESDAY -> TUESDAY;
			case WEDNESDAY -> WEDNESDAY;
			case THURSDAY -> THURSDAY;
			case FRIDAY -> FRIDAY;
			case SATURDAY -> SATURDAY;
			case SUNDAY -> SUNDAY;
		};
	}
}
