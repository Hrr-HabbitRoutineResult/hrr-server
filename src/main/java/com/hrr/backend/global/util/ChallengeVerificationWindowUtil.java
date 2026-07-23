package com.hrr.backend.global.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.hrr.backend.domain.challenge.entity.Challenge;

/**
 * 챌린지의 인증 시간대(자정을 넘어가는 경우 포함) 관련 계산을 공통화한 유틸리티.
 * 기존에는 VerificationServiceImpl에만 이 로직(getWindowAnchorDate)이 있었는데,
 * PointServiceImpl의 주차 퍼펙트 인증 판단 로직도 정확히 동일한 기준으로 "인증일"을 계산해야 해서
 * (자정 넘는 시간대에 인증하면 그 인증은 전날 인증으로 취급되어야 함) 공통 유틸로 추출.
 */
public final class ChallengeVerificationWindowUtil {

    private ChallengeVerificationWindowUtil() {
    }

    /**
     * 인증 시간대 포함 여부 (start <= end 일반 케이스 + start > end 자정 넘어가는 케이스 대응)
     */
    public static boolean isWithinVerificationTime(Challenge challenge, LocalTime now) {
        LocalTime start = challenge.getVerifyStartTime();
        LocalTime end = challenge.getVerifyEndTime();

        // 일반 케이스: 09:00 ~ 22:00
        if (start.isBefore(end) || start.equals(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }

        // 자정 넘어가는 케이스: 22:00 ~ 02:00
        return !now.isBefore(start) || !now.isAfter(end);
    }


    public static LocalDate getWindowAnchorDate(Challenge challenge, LocalDateTime dateTime) {
        LocalTime start = challenge.getVerifyStartTime();
        LocalTime end = challenge.getVerifyEndTime();

        if (start.isBefore(end) || start.equals(end)) {
            return dateTime.toLocalDate();
        }

        // overnight
        LocalTime t = dateTime.toLocalTime();
        if (!t.isBefore(start)) {
            return dateTime.toLocalDate();
        }
        return dateTime.toLocalDate().minusDays(1);
    }
}
