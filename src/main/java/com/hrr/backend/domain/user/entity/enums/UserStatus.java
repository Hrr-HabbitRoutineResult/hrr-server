package com.hrr.backend.domain.user.entity.enums;

public enum UserStatus {
    ACTIVE,   // 활성 (가입 즉시)
    INACTIVE, // 비활성화 (탈퇴 요청 직후)
    SUSPENDED, // 정지 (관리자에 의해)
    DELETED,// 탈퇴 (탈퇴 후 30일이 지나 완전히 삭제된 상태)
}
