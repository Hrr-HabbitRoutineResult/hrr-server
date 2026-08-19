package com.hrr.backend.domain.verification.entity.enums;

public enum VerificationStatus {
    TEMPORARY,
    COMPLETED,
	BLOCKED,
    // 부실인증 신고 이력 보존을 위함
    DELETED
}
