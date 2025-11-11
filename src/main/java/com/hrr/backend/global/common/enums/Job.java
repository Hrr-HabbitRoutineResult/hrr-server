package com.hrr.backend.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Job {

    STUDENT_MIDDLE_HIGH("중고등학생"),
    STUDENT_UNIVERSITY("대학생"),
    JOB_SEEKER("취준생"),
    EMPLOYEE("직장인"),
    HOMEMAKER("주부"),
    ETC("기타");

	private final String description;

}
