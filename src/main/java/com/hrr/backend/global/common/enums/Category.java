package com.hrr.backend.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Category {

	ALL("전체"),
	HEALTH("운동"),
	STUDY("학업"),
	HOBBY("취미"),
	CAREER("취업준비"),
	HABIT("생활습관");

	private final String description;

}
