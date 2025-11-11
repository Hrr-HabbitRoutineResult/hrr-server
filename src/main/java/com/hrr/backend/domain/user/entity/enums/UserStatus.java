package com.hrr.backend.domain.user.entity.enums;

public enum UserStatus {
    ACTIVE,   // 활성 (가입 즉시)
    INACTIVE, // 휴면 (장기 미접속)
    SUSPENDED, // 정지 (관리자에 의해)
    DELETED // 탈퇴
}
