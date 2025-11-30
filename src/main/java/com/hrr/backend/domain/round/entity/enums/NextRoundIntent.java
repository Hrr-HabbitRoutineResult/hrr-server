package com.hrr.backend.domain.round.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NextRoundIntent {

    UNDECIDED("결정 전"), // 기본값
    CONTINUE("계속하기"), // 방장: 연장함 / 팀원: 참여함
    STOP("그만하기");     // 방장: 종료함 / 팀원: 하차함

    private final String description;
}