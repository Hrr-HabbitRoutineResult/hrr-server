package com.hrr.backend.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.hrr.backend.domain.challenge.entity.Challenge;

/**챌린지의 인증 시간대 관련 계산을 공통화한 유틸리티.*/
public final class ChallengeVerificationWindowUtil {

    private ChallengeVerificationWindowUtil() {
    }

    /**
     * 인증 시간대 포함 여부
     */
    public static boolean isWithinVerificationTime(Challenge challenge, LocalTime now) {
        LocalTime start = challenge.getVerifyStartTime();
        LocalTime end = challenge.getVerifyEndTime();

        return !now.isBefore(start) && !now.isAfter(end);
    }

    /**
     * "인증 윈도우 기준 날짜(anchor date)" 계산
     */
    public static LocalDate getWindowAnchorDate(Challenge challenge, LocalDateTime dateTime) {
        return dateTime.toLocalDate();
    }
}