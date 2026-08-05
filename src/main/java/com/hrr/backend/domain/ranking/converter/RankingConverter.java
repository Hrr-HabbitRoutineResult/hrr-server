package com.hrr.backend.domain.ranking.converter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.hrr.backend.domain.ranking.dto.RankingResponseDto;
import com.hrr.backend.domain.ranking.entity.UserRankSnapshot;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.s3.S3UrlUtil;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RankingConverter {

    private final S3UrlUtil s3UrlUtil;

    public RankingResponseDto.BoardDto toBoardDto(
            User user,
            List<UserRankSnapshot> topRankerSnapshots,
            UserRankSnapshot mySnapshot,
            int myRank,
            int totalUserCount,
            int topPercent,
            Integer rankChange,
            String rankChangeMessage
    ) {
        RankingResponseDto.BoardInfoDto board = RankingResponseDto.BoardInfoDto.builder()
                .myRank(myRank)
                .totalUserCount(totalUserCount)
                .topPercent(topPercent)
                .rankChange(rankChange)
                .rankChangeMessage(rankChangeMessage)
                .myPoints(mySnapshot.getPoints())
                .topRankers(toTopRankerDtos(topRankerSnapshots))
                .snapshotDate(mySnapshot.getSnapshotDate())
                .build();

        return RankingResponseDto.BoardDto.builder()
                .myProfile(toMyProfileDto(user))
                .board(board)
                .build();
    }

    /**
     * 아직 랭킹 스냅샷에 내 데이터가 없는 경우(신규 가입자 등)를 위한 응답.
     * 상위 5명은 그대로 보여주되, 나와 관련된 등수/퍼센트/등수변화/포인트는 전부 null로 응답.
     */
    public RankingResponseDto.BoardDto toUnrankedBoardDto(
            User user,
            List<UserRankSnapshot> topRankerSnapshots,
            Integer totalUserCount,
            LocalDate snapshotDate
    ) {
        RankingResponseDto.BoardInfoDto board = RankingResponseDto.BoardInfoDto.builder()
                .myRank(null)
                .totalUserCount(totalUserCount)
                .topPercent(null)
                .rankChange(null)
                .rankChangeMessage(null)
                .myPoints(null)
                .topRankers(toTopRankerDtos(topRankerSnapshots))
                .snapshotDate(snapshotDate)
                .build();

        return RankingResponseDto.BoardDto.builder()
                .myProfile(toMyProfileDto(user))
                .board(board)
                .build();
    }

    /**시스템 전체에 스냅샷이 하나도 없을 때(서비스 오픈 직후, 첫 배치 실행 전 등)의 응답.*/
    public RankingResponseDto.BoardDto toNoSnapshotBoardDto(User user) {
        return RankingResponseDto.BoardDto.builder()
                .myProfile(toMyProfileDto(user))
                .board(null)
                .build();
    }

    /** 위 랭커 목록의 "표시 등수"를 다시 매기기*/
    private List<RankingResponseDto.RankingEntryDto> toTopRankerDtos(List<UserRankSnapshot> snapshots) {
        List<RankingResponseDto.RankingEntryDto> result = new ArrayList<>();

        int displayRank = 0;
        Long previousPoints = null;

        for (int i = 0; i < snapshots.size(); i++) {
            UserRankSnapshot snapshot = snapshots.get(i);

            // 앞 사람과 포인트가 다르면 (현재 순번 + 1)로 등수를 새로 부여, 같으면 앞 사람 등수를 그대로 사용
            if (previousPoints == null || !previousPoints.equals(snapshot.getPoints())) {
                displayRank = i + 1;
            }
            previousPoints = snapshot.getPoints();

            result.add(toRankingEntryDto(snapshot, displayRank));
        }

        return result;
    }

    private RankingResponseDto.MyProfileDto toMyProfileDto(User user) {
        return RankingResponseDto.MyProfileDto.builder()
                .nickname(user.getDisplayNickname())
                .profileImage(s3UrlUtil.toFullUrl(user.getProfileImage()))
                .points(user.getPoints())
                .build();
    }

    private RankingResponseDto.RankingEntryDto toRankingEntryDto(UserRankSnapshot snapshot, int displayRank) {
        User rankedUser = snapshot.getUser();
        return RankingResponseDto.RankingEntryDto.builder()
                .userId(rankedUser.getId())
                .rank(displayRank)
                .nickname(rankedUser.getDisplayNickname())
                .profileImage(s3UrlUtil.toFullUrl(rankedUser.getProfileImage()))
                .points(snapshot.getPoints())
                .build();
    }
}