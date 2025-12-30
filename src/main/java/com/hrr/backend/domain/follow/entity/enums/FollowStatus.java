package com.hrr.backend.domain.follow.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FollowStatus {
    PENDING("대기중"),
    APPROVED("승인됨");

    private final String description;
}