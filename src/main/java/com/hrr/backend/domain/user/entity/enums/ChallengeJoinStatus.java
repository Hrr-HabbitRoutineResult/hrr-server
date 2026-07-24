package com.hrr.backend.domain.user.entity.enums;

public enum ChallengeJoinStatus {
    JOINED,   // 참여 중
    CANCELLED, // 시작 전 나가기
    DROPPED,  // 종료 후 하차
    KICKED    // 비정상 종료(퇴출, 회원 탈퇴 등)
}
