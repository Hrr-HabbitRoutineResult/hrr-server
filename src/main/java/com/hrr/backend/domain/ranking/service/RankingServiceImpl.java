package com.hrr.backend.domain.ranking.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.ranking.converter.RankingConverter;
import com.hrr.backend.domain.ranking.dto.RankingResponseDto;
import com.hrr.backend.domain.ranking.entity.UserRankSnapshot;
import com.hrr.backend.domain.ranking.repository.UserRankSnapshotRepository;
import com.hrr.backend.domain.user.entity.User;

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
        Optional<LocalDate> latestSnapshotDateOpt = userRankSnapshotRepository.findLatestSnapshotDate();

        // 서비스 오픈 직후처럼 아직 첫 배치가 돌지 않은 시점에는 랭킹 탭 자체가 열리지 않는 문제가 있어,
        // 에러 대신 정상 응답으로 내려주고 프론트가 board == null 로 "집계 전" 상태를 판단하도록 변경
        if (latestSnapshotDateOpt.isEmpty()) {
            return rankingConverter.toNoSnapshotBoardDto(user);
        }
        LocalDate latestSnapshotDate = latestSnapshotDateOpt.get();

        List<UserRankSnapshot> topRankers = userRankSnapshotRepository.findTopByRanking(
                latestSnapshotDate, PageRequest.of(0, TOP_N)
        );

        // 전체 인원수를 활성 유저 기준으로 다시 계산
        int totalUserCount = (int) userRankSnapshotRepository.countActiveBySnapshotDate(latestSnapshotDate);

        Optional<UserRankSnapshot> mySnapshotOpt = userRankSnapshotRepository
                .findByUserIdAndSnapshotDate(user.getId(), latestSnapshotDate);

        if (mySnapshotOpt.isEmpty()) {
            // topRankers 첫 행의 저장값 대신, 위에서 계산한 활성 유저 수 사용
            return rankingConverter.toUnrankedBoardDto(user, topRankers, totalUserCount, latestSnapshotDate);
        }

        UserRankSnapshot mySnapshot = mySnapshotOpt.get();

        // 내 등수를 활성 유저 기준으로 계산
        int myRank = (int) userRankSnapshotRepository
                .countHigherRankers(latestSnapshotDate, mySnapshot.getPoints()) + 1;

        // 직전 스냅샷 등수도 같은 기준(활성 유저)으로 계산해서 비교
        Integer rankChange = userRankSnapshotRepository
                .findPreviousSnapshots(user.getId(), latestSnapshotDate, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(previous -> {
                    int previousRank = (int) userRankSnapshotRepository
                            .countHigherRankers(previous.getSnapshotDate(), previous.getPoints()) + 1;
                    return previousRank - myRank; // 양수: 상승, 음수: 하락
                })
                .orElse(null);

        // 상위 퍼센트도 재계산된 등수/전체 인원 기준으로 산출
        int topPercent = totalUserCount > 0
                ? (int) Math.ceil((myRank * 100.0) / totalUserCount)
                : 100;

        String rankChangeMessage = buildRankChangeMessage(rankChange);

        // 재계산된 myRank / totalUserCount 를 컨버터로 전달
        return rankingConverter.toBoardDto(
                user, topRankers, mySnapshot, myRank, totalUserCount, topPercent, rankChange, rankChangeMessage);
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