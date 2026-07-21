package com.hrr.backend.domain.notification.entity.enums;

public enum NotificationTypeName {
    CHALLENGE_START,              // 챌린지 시작 하루 전 안내
    CHALLENGE_UPDATED,            // 챌린지 수정 안내
    CHALLENGE_EXTENSION,          // 챌린지 라운드 연장 안내
    CHALLENGE_EXTENSION_SUCCESS,  // 챌린지 연장 확정
    CHALLENGE_EXTENSION_CANCEL,   // 챌린지 연장 취소

    VERIFICATION_DEADLINE_3H,     // 인증 마감 3시간 전
    VERIFICATION_DEADLINE_1H,     // 인증 마감 1시간 전
    VERIFICATION_DEADLINE_NOW     // 인증 시간이 1시간 미만 → 시작 즉시
}
