package com.hrr.backend.domain.ranking.service;

import com.hrr.backend.domain.ranking.dto.RankingResponseDto;
import com.hrr.backend.domain.user.entity.User;

public interface RankingService {

    // 내 랭킹 보드 조회 (상단 프로필은 실시간, 랭킹보드는 주간 스냅샷 기준)
    RankingResponseDto.BoardDto getMyRankingBoard(User user);
}