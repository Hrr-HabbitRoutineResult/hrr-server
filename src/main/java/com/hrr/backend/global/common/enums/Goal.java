package com.hrr.backend.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Goal {

    BUILD_EXERCISE_HABIT("운동 습관 만들기", "운동습관"),
    HEALTHY_DAY("건강한 하루 챙기기", "건강"),
    EXAM_CAREER_PREP("시험, 취업 준비하기", "준비"),
    FIND_NEW_HOBBY("새로운 취미 발견하기", "취미발견"),
    ENJOY_HOBBY_TOGETHER("함께 취미 즐기기", "취미공유"),
    FOCUS_ON_MYSELF("나에게 몰입하기", "몰입"),
    KEEP_GOING("꾸준함 이어가기", "꾸준함");

	private final String description;
    private final String label;

}
