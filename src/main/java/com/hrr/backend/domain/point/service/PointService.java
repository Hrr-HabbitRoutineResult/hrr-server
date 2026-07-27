package com.hrr.backend.domain.point.service;

import java.time.LocalDateTime;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.point.dto.PointHistoryResponseDto;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.RandomMission;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.global.response.SliceResponseDto;

public interface PointService {

    // 챌린지 첫 인증 포인트 지급 (챌린지당 최초 1회, 내부에서 중복 지급 방지)
    void earnFirstVerificationPoint(User user, Challenge challenge, Verification verification);

    // 랜덤미션 참여 포인트 지급 (호출부에서 일 1회 제한을 보장해야 함)
    void earnRandomMissionPoint(User user, RandomMission randomMission);

    // 종료된 라운드에 속한 모든 참여자를 대상으로, 미인증 없이 완주했다면 포인트 지급
    void checkAndEarnFlawlessRoundPoints(Round endedRound);

    // 동일 챌린지 3라운드 이상 참여 시(최초 1회) 포인트 지급 여부 확인 및 지급
    void checkAndEarnChallengeMasterPoint(User user, Challenge challenge, UserChallenge userChallenge);

    // 이번 인증이 그 주의 마지막 인증일이고, 해당 주가 결석 없이 퍼펙트였다면 포인트 지급
    void checkAndEarnWeeklyPerfectPoint(
            UserChallenge userChallenge,
            RoundRecord roundRecord,
            Round round,
            Challenge challenge,
            LocalDateTime verifiedAt,
            Verification verification
    );

    // 특정 인증글로 인해 지급된 포인트(FIRST_VERIFICATION, WEEKx_PERFECT)를 전부 찾아 삭제하고, 유저 포인트를 그만큼 원자적으로 차감
    void revokePointsForVerification(Verification verification);

    void awardVerificationTriggeredPoints(Long verificationId);

    // 내 포인트 적립 내역 조회 (최근 3개월, 무한 스크롤) + 현재 보유 포인트
    PointHistoryResponseDto.PageDto getMyPointHistory(User user, int page, int size);
}