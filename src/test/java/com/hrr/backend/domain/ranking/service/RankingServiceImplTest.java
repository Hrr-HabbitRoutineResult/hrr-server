package com.hrr.backend.domain.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    @DisplayName("아직 랭킹 스냅샷이 한 번도 생성되지 않았으면 에러 대신 board=null로 응답한다")
    void getMyRankingBoard_returnsNullBoard_whenNoSnapshotExists() {
        // given
        rankingService = new RankingServiceImpl(userRankSnapshotRepository, rankingConverter);
        User user = User.builder().id(1L).nickname("tester").points(0L).build();

        given(userRankSnapshotRepository.findLatestSnapshotDate()).willReturn(Optional.empty());

        // when
        RankingResponseDto.BoardDto result = rankingService.getMyRankingBoard(user);

        // then: 랭킹 보드는 null이지만, 상단 프로필은 실시간 값으로 정상 응답
        assertThat(result.getBoard()).isNull();
        assertThat(result.getMyProfile().getNickname()).isEqualTo("tester");
        assertThat(result.getMyProfile().getPoints()).isEqualTo(0L);
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
        // 전체 인원수는 스냅샷 저장값이 아니라 활성 유저 기준 COUNT로 재계산된다
        given(userRankSnapshotRepository.countActiveBySnapshotDate(latest)).willReturn(132L);
        given(userRankSnapshotRepository.findByUserIdAndSnapshotDate(1L, latest)).willReturn(Optional.empty());

        // when
        RankingResponseDto.BoardDto result = rankingService.getMyRankingBoard(user);

        // then: 에러 없이 200 응답, 나와 관련된 필드는 전부 null
        assertThat(result.getBoard().getMyRank()).isNull();
        assertThat(result.getBoard().getTopPercent()).isNull();
        assertThat(result.getBoard().getRankChange()).isNull();
        assertThat(result.getBoard().getRankChangeMessage()).isNull();
        assertThat(result.getBoard().getMyPoints()).isNull();
        // 전체 인원수는 상위 랭커 스냅샷 값이 아니라 countActiveBySnapshotDate 결과다
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
        // 등수/전체인원은 저장값이 아니라 COUNT 쿼리로 재계산되므로 스텁이 필요하다.
        //        myRank      = countHigherRankers(latest, 100) + 1 = 89 + 1 = 90
        //        previousRank= countHigherRankers(previous, 80) + 1 = 99 + 1 = 100
        given(userRankSnapshotRepository.countActiveBySnapshotDate(latest)).willReturn(1000L);
        given(userRankSnapshotRepository.countHigherRankers(latest, 100L)).willReturn(89L);
        given(userRankSnapshotRepository.countHigherRankers(previous, 80L)).willReturn(99L);

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
        // myRank = 49 + 1 = 50 / previousRank = 39 + 1 = 40
        given(userRankSnapshotRepository.countActiveBySnapshotDate(latest)).willReturn(1000L);
        given(userRankSnapshotRepository.countHigherRankers(latest, 80L)).willReturn(49L);
        given(userRankSnapshotRepository.countHigherRankers(previous, 90L)).willReturn(39L);

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
        // myRank = 29 + 1 = 30, 전체 100명
        given(userRankSnapshotRepository.countActiveBySnapshotDate(latest)).willReturn(100L);
        given(userRankSnapshotRepository.countHigherRankers(latest, 100L)).willReturn(29L);

        // when
        RankingResponseDto.BoardDto result = rankingService.getMyRankingBoard(user);

        // then
        assertThat(result.getBoard().getRankChange()).isNull();
        assertThat(result.getBoard().getRankChangeMessage()).isNull();
        // 상위 퍼센트: 30 / 100 * 100 = 30(올림)
        assertThat(result.getBoard().getTopPercent()).isEqualTo(30);
    }

    // ensureWeeklySnapshot() 테스트 2개
    //  이 메서드가 "이미 있으면 절대 덮어쓰지 않는다"는 점이 핵심
    //  30초마다 호출되는데 덮어쓰기가 일어나면 '월요일에 고정된 값'이라는 스펙이 깨지기 때문에,
    //  스킵 동작을 반드시 테스트로 고정해 둔다.
    @Test
    @DisplayName("ensureWeeklySnapshot: 이미 해당 주 스냅샷이 있으면 생성하지 않고 0을 반환한다")
    void ensureWeeklySnapshot_skips_whenSnapshotAlreadyExists() {
        // given
        rankingService = new RankingServiceImpl(userRankSnapshotRepository, rankingConverter);
        LocalDate monday = LocalDate.of(2026, 8, 17);

        given(userRankSnapshotRepository.existsBySnapshotDate(monday)).willReturn(true);

        // when
        int created = rankingService.ensureWeeklySnapshot(monday);

        // then: INSERT는 절대 호출되지 않아야 한다 (월요일 확정값 덮어쓰기 방지)
        assertThat(created).isZero();
        verify(userRankSnapshotRepository, never()).insertWeeklySnapshot(any(), any());
    }

    @Test
    @DisplayName("ensureWeeklySnapshot: 스냅샷이 없으면 '월요일 00:00 미만' 컷오프로 생성한다")
    void ensureWeeklySnapshot_insertsWithMidnightCutoff_whenSnapshotMissing() {
        // given
        rankingService = new RankingServiceImpl(userRankSnapshotRepository, rankingConverter);
        LocalDate monday = LocalDate.of(2026, 8, 17);
        // 2026-08-16(일) 23:59:59.999999 까지의 포인트만 집계되도록 하는 컷오프
        LocalDateTime expectedCutoff = LocalDateTime.of(2026, 8, 17, 0, 0);

        given(userRankSnapshotRepository.existsBySnapshotDate(monday)).willReturn(false);
        given(userRankSnapshotRepository.insertWeeklySnapshot(monday, expectedCutoff)).willReturn(120);

        // when
        int created = rankingService.ensureWeeklySnapshot(monday);

        // then
        assertThat(created).isEqualTo(120);
        verify(userRankSnapshotRepository).insertWeeklySnapshot(monday, expectedCutoff);
    }
}