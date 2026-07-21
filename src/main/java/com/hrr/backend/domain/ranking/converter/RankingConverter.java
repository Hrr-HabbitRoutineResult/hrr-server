package com.hrr.backend.domain.ranking.converter;

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