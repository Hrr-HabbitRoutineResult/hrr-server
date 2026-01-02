package com.hrr.backend.domain.user.entity.enums;

public enum UserStatus {
    ACTIVE,   // 활성 (가입 즉시)
    INACTIVE, // 휴면 (장기 미접속)
    SUSPENDED, // 정지 (관리자에 의해)
    DELETED, // 탈퇴 (탈퇴 요청 시 해당 상태로 변경; soft delete)
	WITHDRAWN_COMPLETED // 탈퇴 완료 (탈퇴 후 30일이 지나 완전히 삭제된 상태)
}
