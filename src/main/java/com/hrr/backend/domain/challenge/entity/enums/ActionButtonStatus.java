package com.hrr.backend.domain.challenge.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActionButtonStatus {
    CERTIFIED("인증 완료"),
    CERTIFY_AVAILABLE("인증하기"),
    JOIN("참가하기"),
    WAITLIST("빈자리 알림 받기"),
    DISABLED("참가 불가");

    private final String description;
}