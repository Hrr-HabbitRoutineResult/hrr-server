package com.hrr.backend.domain.ranking.converter;

import java.time.LocalDate;
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
            int topPercent,
            Integer rankChange,
            String rankChangeMessage
    ) {
        RankingResponseDto.MyProfileDto myProfile = RankingResponseDto.MyProfileDto.builder()
                .nickname(user.getDisplayNickname())
                .profileImage(s3UrlUtil.toFullUrl(user.getProfileImage()))
                .points(user.getPoints())
                .build();

        List<RankingResponseDto.RankingEntryDto> topRankers = topRankerSnapshots.stream()
                .map(this::toRankingEntryDto)
                .toList();

        RankingResponseDto.BoardInfoDto board = RankingResponseDto.BoardInfoDto.builder()
                .myRank(mySnapshot.getRanking())
                .totalUserCount(mySnapshot.getTotalUserCount())
                .topPercent(topPercent)
                .rankChange(rankChange)
                .rankChangeMessage(rankChangeMessage)
                .myPoints(mySnapshot.getPoints())
                .topRankers(topRankers)
                .snapshotDate(mySnapshot.getSnapshotDate())
                .build();

        return RankingResponseDto.BoardDto.builder()
                .myProfile(myProfile)
                .board(board)
                .build();
    }

    /**
     * 아직 랭킹 스냅샷에 내 데이터가 없는 경우(신규 가입자 등)를 위한 응답.
     * 상위 5명은 그대로 보여주되, 나와 관련된 등수/퍼센트/등수변화/포인트는 전부 null로 응답한다.
     */
    public RankingResponseDto.BoardDto toUnrankedBoardDto(
            User user,
            List<UserRankSnapshot> topRankerSnapshots,
            Integer totalUserCount,
            LocalDate snapshotDate
    ) {
        List<RankingResponseDto.RankingEntryDto> topRankers = topRankerSnapshots.stream()
                .map(this::toRankingEntryDto)
                .toList();

        RankingResponseDto.BoardInfoDto board = RankingResponseDto.BoardInfoDto.builder()
                .myRank(null)
                .totalUserCount(totalUserCount)
                .topPercent(null)
                .rankChange(null)
                .rankChangeMessage(null)
                .myPoints(null)
                .topRankers(topRankers)
                .snapshotDate(snapshotDate)
                .build();

        return RankingResponseDto.BoardDto.builder()
                .myProfile(toMyProfileDto(user))
                .board(board)
                .build();
    }

    private RankingResponseDto.MyProfileDto toMyProfileDto(User user) {
        return RankingResponseDto.MyProfileDto.builder()
                .nickname(user.getDisplayNickname())
                .profileImage(s3UrlUtil.toFullUrl(user.getProfileImage()))
                .points(user.getPoints())
                .build();
    }

    private RankingResponseDto.RankingEntryDto toRankingEntryDto(UserRankSnapshot snapshot) {
        User rankedUser = snapshot.getUser();
        return RankingResponseDto.RankingEntryDto.builder()
                .userId(rankedUser.getId())
                .rank(snapshot.getRanking())
                .nickname(rankedUser.getDisplayNickname())
                .profileImage(s3UrlUtil.toFullUrl(rankedUser.getProfileImage()))
                .points(snapshot.getPoints())
                .build();
    }
}