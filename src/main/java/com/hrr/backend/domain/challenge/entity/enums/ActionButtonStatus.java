package com.hrr.backend.domain.challenge.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActionButtonStatus {
    // 1. 참여자용 상태
    AVAILABLE("인증하기"),
    DONE("인증 완료"),
    UPCOMING("라운드 시작 전"),
    NOT_DAY("인증 요일 아님"),
    NOT_TIME("인증 시간 아님"),

    // 2. 미참여자용 상태
    JOIN("참가하기"),
    WAITLIST("빈자리 알림 받기"),

    // 3. 제한 상태 (DISABLED 세분화)
    FINISHED("종료된 챌린지"),
    MAX_LIMIT_EXCEEDED("참여 개수 초과"),
    REJECT("참가 신청 불가");

    private final String description;
}