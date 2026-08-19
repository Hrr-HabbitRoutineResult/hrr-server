package com.hrr.backend.domain.ranking.service;

import java.time.LocalDate;

import com.hrr.backend.domain.ranking.dto.RankingResponseDto;
import com.hrr.backend.domain.user.entity.User;

public interface RankingService {

    // 내 랭킹 보드 조회 (상단 프로필은 실시간, 랭킹보드는 주간 스냅샷 기준)
    RankingResponseDto.BoardDto getMyRankingBoard(User user);

    // 주간 랭킹 스냅샷 보장(catch-up)
    /**지정한 월요일자 스냅샷이 없으면 생성한다. (이미 있으면 아무것도 하지 않음)*/
    boolean ensureWeeklySnapshot(LocalDate snapshotDate);
}