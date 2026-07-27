package com.hrr.backend.domain.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrr.backend.domain.ranking.converter.RankingConverter;
import com.hrr.backend.domain.ranking.dto.RankingResponseDto;
import com.hrr.backend.domain.ranking.entity.UserRankSnapshot;
import com.hrr.backend.domain.ranking.repository.UserRankSnapshotRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import com.hrr.backend.global.s3.S3UrlUtil;

@ExtendWith(MockitoExtension.class)
class RankingServiceImplTest {

    @InjectMocks
    private RankingServiceImpl rankingService;

    @Mock
    private UserRankSnapshotRepository userRankSnapshotRepository;

    // RankingServiceImpl은 RankingConverter를 통해 DTO를 만들기 때문에,
    // 실제 변환 로직까지 검증하기 위해 Mock이 아닌 실제 컨버터 인스턴스를 사용한다.
    private final RankingConverter rankingConverter = new RankingConverter(new S3UrlUtil("https://example.com"));

    @Test
    @DisplayName("아직 랭킹 스냅샷이 한 번도 생성되지 않았으면 예외가 발생한다")
    void getMyRankingBoard_throws_whenNoSnapshotExists() {
        // given
        rankingService = new RankingServiceImpl(userRankSnapshotRepository, rankingConverter);
        User user = User.builder().id(1L).nickname("tester").points(0L).build();

        given(userRankSnapshotRepository.findLatestSnapshotDate()).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> rankingService.getMyRankingBoard(user))
                .isInstanceOf(GlobalException.class)
                .extracting(e -> ((GlobalException) e).getErrorCode())
                .isEqualTo(ErrorCode.RANKING_SNAPSHOT_NOT_FOUND);
    }

    @Test
    @DisplayName("전체 스냅샷은 있지만 내 스냅샷이 없으면(신규 가입자 등) 에러 대신 null 필드로 응답한다")
    void getMyRankingBoard_returnsNullFields_whenMySnapshotMissing() {
        // given
        rankingService = new RankingServiceImpl(userRankSnapshotRepository, rankingConverter);

        User user = User.builder().id(1L).nickname("신규유저").points(0L).build();
        LocalDate latest = LocalDate.of(2026, 7, 20);

        User topUser = User.builder().id(9101L).nickname("라인").points(2700L).build();
        UserRankSnapshot topSnapshot = UserRankSnapshot.builder()
                .id(1L).user(topUser).ranking(1).points(2700L).totalUserCount(132).snapshotDate(latest)
                .build();

        given(userRankSnapshotRepository.findLatestSnapshotDate()).willReturn(Optional.of(latest));
        given(userRankSnapshotRepository.findTopByRanking(eq(latest), any())).willReturn(List.of(topSnapshot));
        given(userRankSnapshotRepository.findByUserIdAndSnapshotDate(1L, latest)).willReturn(Optional.empty());

        // when
        RankingResponseDto.BoardDto result = rankingService.getMyRankingBoard(user);

        // then: 에러 없이 200 응답, 나와 관련된 필드는 전부 null
        assertThat(result.getBoard().getMyRank()).isNull();
        assertThat(result.getBoard().getTopPercent()).isNull();
        assertThat(result.getBoard().getRankChange()).isNull();
        assertThat(result.getBoard().getRankChangeMessage()).isNull();
        assertThat(result.getBoard().getMyPoints()).isNull();
        // 상위 5명은 그대로 노출되고, 전체 인원수는 상위 랭커의 스냅샷 값에서 가져옴
        assertThat(result.getBoard().getTopRankers()).hasSize(1);
        assertThat(result.getBoard().getTotalUserCount()).isEqualTo(132);
        assertThat(result.getBoard().getSnapshotDate()).isEqualTo(latest);
        // 상단 프로필 영역은 실시간 값 그대로 노출
        assertThat(result.getMyProfile().getNickname()).isEqualTo("신규유저");
    }

    @Test
    @DisplayName("직전 스냅샷이 있으면 등수 변화(rankChange)가 계산된다 - 등수가 낮아지면(숫자가 작아지면) 상승")
    void getMyRankingBoard_calculatesPositiveRankChange_whenRankImproved() {
        // given
        rankingService = new RankingServiceImpl(userRankSnapshotRepository, rankingConverter);

        User user = User.builder().id(1L).nickname("김흐르").points(100L).build();
        LocalDate latest = LocalDate.of(2026, 7, 20);
        LocalDate previous = LocalDate.of(2026, 7, 13);

        UserRankSnapshot mySnapshot = UserRankSnapshot.builder()
                .id(1L).user(user).ranking(90).points(100L).totalUserCount(1000).snapshotDate(latest)
                .build();
        UserRankSnapshot previousSnapshot = UserRankSnapshot.builder()
                .id(2L).user(user).ranking(100).points(80L).totalUserCount(1000).snapshotDate(previous)
                .build();

        given(userRankSnapshotRepository.findLatestSnapshotDate()).willReturn(Optional.of(latest));
        given(userRankSnapshotRepository.findTopByRanking(eq(latest), any())).willReturn(List.of());
        given(userRankSnapshotRepository.findByUserIdAndSnapshotDate(1L, latest)).willReturn(Optional.of(mySnapshot));
        given(userRankSnapshotRepository.findPreviousSnapshots(eq(1L), eq(latest), any()))
                .willReturn(List.of(previousSnapshot));

        // when
        RankingResponseDto.BoardDto result = rankingService.getMyRankingBoard(user);

        // then: 100위 -> 90위이므로 10계단 상승 (양수)
        assertThat(result.getBoard().getRankChange()).isEqualTo(10);
        assertThat(result.getBoard().getMyRank()).isEqualTo(90);
        assertThat(result.getMyProfile().getPoints()).isEqualTo(100L); // 실시간 포인트는 User 엔티티 값 그대로
        assertThat(result.getBoard().getRankChangeMessage()).isEqualTo("지난주보다 10계단 상승했어요");
    }

    @Test
    @DisplayName("직전 스냅샷보다 등수가 나빠지면(숫자가 커지면) 하락 문구가 내려간다")
    void getMyRankingBoard_calculatesNegativeRankChange_whenRankDropped() {
        // given
        rankingService = new RankingServiceImpl(userRankSnapshotRepository, rankingConverter);

        User user = User.builder().id(1L).nickname("김흐르").points(100L).build();
        LocalDate latest = LocalDate.of(2026, 7, 20);
        LocalDate previous = LocalDate.of(2026, 7, 13);

        UserRankSnapshot mySnapshot = UserRankSnapshot.builder()
                .id(1L).user(user).ranking(50).points(80L).totalUserCount(1000).snapshotDate(latest)
                .build();
        UserRankSnapshot previousSnapshot = UserRankSnapshot.builder()
                .id(2L).user(user).ranking(40).points(90L).totalUserCount(1000).snapshotDate(previous)
                .build();

        given(userRankSnapshotRepository.findLatestSnapshotDate()).willReturn(Optional.of(latest));
        given(userRankSnapshotRepository.findTopByRanking(eq(latest), any())).willReturn(List.of());
        given(userRankSnapshotRepository.findByUserIdAndSnapshotDate(1L, latest)).willReturn(Optional.of(mySnapshot));
        given(userRankSnapshotRepository.findPreviousSnapshots(eq(1L), eq(latest), any()))
                .willReturn(List.of(previousSnapshot));

        // when
        RankingResponseDto.BoardDto result = rankingService.getMyRankingBoard(user);

        // then: 40위 -> 50위이므로 10계단 하락 (음수)
        assertThat(result.getBoard().getRankChange()).isEqualTo(-10);
        assertThat(result.getBoard().getRankChangeMessage()).isEqualTo("지난주보다 10계단 하락했어요");
    }

    @Test
    @DisplayName("직전 스냅샷이 없으면(첫 주) rankChange는 null이다")
    void getMyRankingBoard_rankChangeIsNull_whenNoPreviousSnapshot() {
        // given
        rankingService = new RankingServiceImpl(userRankSnapshotRepository, rankingConverter);

        User user = User.builder().id(1L).nickname("김흐르").points(100L).build();
        LocalDate latest = LocalDate.of(2026, 7, 20);

        UserRankSnapshot mySnapshot = UserRankSnapshot.builder()
                .id(1L).user(user).ranking(30).points(100L).totalUserCount(100).snapshotDate(latest)
                .build();

        given(userRankSnapshotRepository.findLatestSnapshotDate()).willReturn(Optional.of(latest));
        given(userRankSnapshotRepository.findTopByRanking(eq(latest), any())).willReturn(List.of());
        given(userRankSnapshotRepository.findByUserIdAndSnapshotDate(1L, latest)).willReturn(Optional.of(mySnapshot));
        given(userRankSnapshotRepository.findPreviousSnapshots(eq(1L), eq(latest), any()))
                .willReturn(List.of());

        // when
        RankingResponseDto.BoardDto result = rankingService.getMyRankingBoard(user);

        // then
        assertThat(result.getBoard().getRankChange()).isNull();
        assertThat(result.getBoard().getRankChangeMessage()).isNull();
        // 상위 퍼센트: 30 / 100 * 100 = 30(올림)
        assertThat(result.getBoard().getTopPercent()).isEqualTo(30);
    }
}