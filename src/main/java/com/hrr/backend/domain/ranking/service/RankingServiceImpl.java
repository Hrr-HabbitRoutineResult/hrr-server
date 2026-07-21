package com.hrr.backend.domain.ranking.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.ranking.converter.RankingConverter;
import com.hrr.backend.domain.ranking.dto.RankingResponseDto;
import com.hrr.backend.domain.ranking.entity.UserRankSnapshot;
import com.hrr.backend.domain.ranking.repository.UserRankSnapshotRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingServiceImpl implements RankingService {

    // 랭킹보드에 노출할 상위 랭커 수
    private static final int TOP_N = 5;

    private final UserRankSnapshotRepository userRankSnapshotRepository;
    private final RankingConverter rankingConverter;

    @Override
    @Transactional(readOnly = true)
    public RankingResponseDto.BoardDto getMyRankingBoard(User user) {
        // 가장 최근 스냅샷 기준일 조회
        LocalDate latestSnapshotDate = userRankSnapshotRepository.findLatestSnapshotDate()
                .orElseThrow(() -> new GlobalException(ErrorCode.RANKING_SNAPSHOT_NOT_FOUND));
        List<UserRankSnapshot> topRankers = userRankSnapshotRepository.findTopByRanking(
                latestSnapshotDate, PageRequest.of(0, TOP_N)
        );

        UserRankSnapshot mySnapshot = userRankSnapshotRepository
                .findByUserIdAndSnapshotDate(user.getId(), latestSnapshotDate)
                .orElseThrow(() -> new GlobalException(ErrorCode.RANKING_MY_SNAPSHOT_NOT_FOUND));

        // 직전 스냅샷과 비교하여 등수 변화 계산 (없으면 null = 비교 데이터 없음)
        Integer rankChange = userRankSnapshotRepository
                .findPreviousSnapshots(user.getId(), latestSnapshotDate, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(previous -> previous.getRanking() - mySnapshot.getRanking()) // 양수: 상승, 음수: 하락
                .orElse(null);

        int topPercent = (int) Math.ceil((mySnapshot.getRanking() * 100.0) / mySnapshot.getTotalUserCount());

        String rankChangeMessage = buildRankChangeMessage(rankChange);

        return rankingConverter.toBoardDto(user, topRankers, mySnapshot, topPercent, rankChange, rankChangeMessage);
    }

    // rankChange 값을 기반으로 문구 생성 (이전 데이터 없으면 null)
    private String buildRankChangeMessage(Integer rankChange) {
        if (rankChange == null) {
            return null;
        }
        if (rankChange > 0) {
            return "지난주보다 " + rankChange + "계단 상승했어요";
        }
        if (rankChange < 0) {
            return "지난주보다 " + Math.abs(rankChange) + "계단 하락했어요";
        }
        return "지난주와 순위가 같아요";
    }
}