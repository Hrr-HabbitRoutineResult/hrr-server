package com.hrr.backend.domain.challenge.entity.enums;

public enum ExtensionStatus {
    NONE,       // 연장 가능 기간이 아니거나 연장 대상이 아님
    PENDING,    // 연장 가능 기간이며 다음 라운드 참여 여부를 아직 선택하지 않음
    COMPLETED   // 다음 라운드 참여 여부를 선택 완료함
}
