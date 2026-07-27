package com.hrr.backend.domain.point.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PointType {

    FIRST_VERIFICATION(1, "챌린지 첫 인증"),
    RANDOM_MISSION(1, "랜덤미션 참여"),
    FLAWLESS_ROUND(3, "무결석 완주"),
    CHALLENGE_MASTER(5, "챌린지 마스터"),
    WEEK1_PERFECT(1, "1주차 퍼펙트 인증"),
    WEEK2_PERFECT(2, "2주차 퍼펙트 인증"),
    WEEK3_PERFECT(3, "3주차 퍼펙트 인증");

    private final int points;
    private final String description;
}