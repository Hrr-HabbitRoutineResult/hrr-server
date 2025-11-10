package com.hrr.backend.global.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AvailableTime {

    EARLY_MORNING("05-09"),
    MORNING("09-12"),
    LUNCH("12-14"),
    AFTERNOON("14-18"),
    EVENING("18-21"),
    NIGHT("21-24"),
    LATE_NIGHT("00-05");

	private final String description;

}
