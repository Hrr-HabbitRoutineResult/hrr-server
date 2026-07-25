package com.hrr.backend.domain.round;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.round.converter.RoundConverter;
import com.hrr.backend.domain.round.dto.RoundDecisionRequestDto;
import com.hrr.backend.domain.round.dto.RoundDecisionResponseDto;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.round.service.RoundDecisionServiceImpl;
import com.hrr.backend.domain.round.service.RoundDropServiceImpl;
import com.hrr.backend.domain.round.service.RoundLifecycleServiceImpl;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class RoundFlowUnitTest {

    @Mock ChallengeRepository challengeRepository;
    @Mock UserChallengeRepository userChallengeRepository;
    @Mock RoundRepository roundRepository;
    @Mock RoundRecordRepository roundRecordRepository;
    @Mock RoundConverter roundConverter;
    @Mock TransactionTemplate transactionTemplate;
    @Mock ApplicationEventPublisher publisher;

    @InjectMocks RoundDecisionServiceImpl roundDecisionService;
    @InjectMocks RoundDropServiceImpl roundDropService;
    @InjectMocks RoundLifecycleServiceImpl roundLifecycleService;

    private void mockTransaction() {
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null); // 트랜잭션 내부 로직 실행
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    // =========================================================================
    // 추가: 연장 가능 기간 검증 테스트 (D-3 ~ D-2)
    // =========================================================================

    @Test
    @DisplayName("[수정된 로직] D-4일에는 연장 불가 (아직 열리지 않음)")
    void decideNextRound_D_minus_4_shouldFail_notOpenYet() {
        // given
        Long userId = 1L;
        Long challengeId = 10L;

        // 수정: 현재 시점 기준 상대 날짜로 설정
        LocalDate today = LocalDate.now(); // 현재 날짜
        LocalDate endDate = today.plusDays(4); // 4일 후 종료

        Challenge challenge = mock(Challenge.class);
        Round currentRound = mock(Round.class);
        UserChallenge uc = mock(UserChallenge.class);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)).thenReturn(Optional.of(uc));
        when(uc.getStatus()).thenReturn(ChallengeJoinStatus.JOINED);
        when(challenge.getCurrentRound()).thenReturn(currentRound);

        when(currentRound.getStartDate()).thenReturn(today.minusDays(10));
        when(currentRound.getEndDate()).thenReturn(endDate);

        RoundDecisionRequestDto request = new RoundDecisionRequestDto(NextRoundIntent.CONTINUE, null);

        // when & then
        assertThatThrownBy(() -> roundDecisionService.decideNextRound(userId, challengeId, request))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROUND_DECISION_PERIOD_NOT_OPEN);

        verifyNoInteractions(roundRecordRepository);
    }

    @Test
    @DisplayName(" [수정된 로직] D-3일에는 연장 가능 (기간 시작)")
    void decideNextRound_D_minus_3_shouldSucceed() {
        // given
        Long userId = 1L;
        Long challengeId = 10L;

        // 수정: 현재 시점 기준 상대 날짜로 설정
        LocalDate today = LocalDate.now(); // 현재 날짜
        LocalDate endDate = today.plusDays(3); // 3일 후 종료

        Challenge challenge = mock(Challenge.class);
        Round currentRound = mock(Round.class);
        UserChallenge uc = mock(UserChallenge.class);
        RoundRecord rr = mock(RoundRecord.class);
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)).thenReturn(Optional.of(uc));
        when(uc.getStatus()).thenReturn(ChallengeJoinStatus.JOINED);
        when(challenge.getCurrentRound()).thenReturn(currentRound);
        when(currentRound.getId()).thenReturn(100L);

        when(currentRound.getStartDate()).thenReturn(today.minusDays(10));
        when(currentRound.getEndDate()).thenReturn(endDate);

        when(roundRecordRepository.findByUserChallengeAndRoundId(uc, 100L)).thenReturn(Optional.of(rr));

        RoundDecisionRequestDto request = new RoundDecisionRequestDto(NextRoundIntent.CONTINUE, null);

        // when
        RoundDecisionResponseDto response = roundDecisionService.decideNextRound(userId, challengeId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isResponded()).isTrue();
        verify(rr).updateNextRoundIntent(NextRoundIntent.CONTINUE);
    }

    @Test
    @DisplayName("[수정된 로직] D-2일에도 연장 가능  (기간 마지막 날)")
    void decideNextRound_D_minus_2_shouldSucceed() {
        // given
        Long userId = 1L;
        Long challengeId = 10L;

        //수정: 현재 시점 기준 상대 날짜로 설정
        LocalDate today = LocalDate.now(); // 현재 날짜
        LocalDate endDate = today.plusDays(2); // 2일 후 종료

        Challenge challenge = mock(Challenge.class);
        Round currentRound = mock(Round.class);
        UserChallenge uc = mock(UserChallenge.class);
        RoundRecord rr = mock(RoundRecord.class);
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)).thenReturn(Optional.of(uc));
        when(uc.getStatus()).thenReturn(ChallengeJoinStatus.JOINED);
        when(challenge.getCurrentRound()).thenReturn(currentRound);
        when(currentRound.getId()).thenReturn(100L);

        when(currentRound.getStartDate()).thenReturn(today.minusDays(10));
        when(currentRound.getEndDate()).thenReturn(endDate);

        when(roundRecordRepository.findByUserChallengeAndRoundId(uc, 100L)).thenReturn(Optional.of(rr));

        RoundDecisionRequestDto request = new RoundDecisionRequestDto(NextRoundIntent.STOP, null);

        // when
        RoundDecisionResponseDto response = roundDecisionService.decideNextRound(userId, challengeId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isResponded()).isTrue();
        verify(rr).updateNextRoundIntent(NextRoundIntent.STOP);
    }

    @Test
    @DisplayName("[수정된 로직] D-1일에는 연장 불가 (기간 만료)")
    void decideNextRound_D_minus_1_shouldFail_periodClosed() {
        // given
        Long userId = 1L;
        Long challengeId = 10L;

        // 수정: 현재 시점 기준 상대 날짜로 설정
        LocalDate today = LocalDate.now(); // 현재 날짜
        LocalDate endDate = today.plusDays(1); // 1일 후 종료

        Challenge challenge = mock(Challenge.class);
        Round currentRound = mock(Round.class);
        UserChallenge uc = mock(UserChallenge.class);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)).thenReturn(Optional.of(uc));
        when(uc.getStatus()).thenReturn(ChallengeJoinStatus.JOINED);
        when(challenge.getCurrentRound()).thenReturn(currentRound);

        when(currentRound.getStartDate()).thenReturn(today.minusDays(10));
        when(currentRound.getEndDate()).thenReturn(endDate);

        RoundDecisionRequestDto request = new RoundDecisionRequestDto(NextRoundIntent.CONTINUE, null);

        // when & then
        assertThatThrownBy(() -> roundDecisionService.decideNextRound(userId, challengeId, request))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROUND_DECISION_PERIOD_CLOSED);

        verifyNoInteractions(roundRecordRepository);
    }

    @Test
    @DisplayName("[실제 시나리오] endDate=오늘+3일, 오늘(D-3) 오전 9시 알림 후 즉시 연장 가능")
    void decideNextRound_realScenario_day18_afterNotification() {
        // given
        Long userId = 1L;
        Long challengeId = 10L;

        // 수정: 현재 시점 기준 상대 날짜로 설정
        // 실제 시나리오: 라운드가 3일 후에 끝남
        // 오늘(D-3) 오전 9시에 알림이 발송되고, 사용자가 바로 응답
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(3);

        Challenge challenge = mock(Challenge.class);
        Round currentRound = mock(Round.class);
        UserChallenge uc = mock(UserChallenge.class);
        RoundRecord rr = mock(RoundRecord.class);
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)).thenReturn(Optional.of(uc));
        when(uc.getStatus()).thenReturn(ChallengeJoinStatus.JOINED);
        when(challenge.getCurrentRound()).thenReturn(currentRound);
        when(currentRound.getId()).thenReturn(100L);

        when(currentRound.getStartDate()).thenReturn(today.minusDays(10));
        when(currentRound.getEndDate()).thenReturn(endDate);

        when(roundRecordRepository.findByUserChallengeAndRoundId(uc, 100L)).thenReturn(Optional.of(rr));

        RoundDecisionRequestDto request = new RoundDecisionRequestDto(NextRoundIntent.CONTINUE, null);

        // when
        RoundDecisionResponseDto response = roundDecisionService.decideNextRound(userId, challengeId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.roundId()).isEqualTo(100L);
        assertThat(response.intent()).isEqualTo(NextRoundIntent.CONTINUE);
        assertThat(response.isResponded()).isTrue();

        verify(rr).updateNextRoundIntent(NextRoundIntent.CONTINUE);
    }

    // =========================================================================
    // 1) RoundDecisionServiceImpl 기존 테스트
    // =========================================================================

    @Test
    @DisplayName("decideNextRound: intent가 null/UNDECIDED면 ROUND_DECISION_INTENT_INVALID 예외")
    void decideNextRound_throws_whenIntentInvalid() {
        // given
        Long userId = 1L;
        Long challengeId = 10L;
        LocalDate today = LocalDate.now();

        Challenge challenge = mock(Challenge.class);
        Round currentRound = mock(Round.class);
        UserChallenge uc = mock(UserChallenge.class);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)).thenReturn(Optional.of(uc));
        when(uc.getStatus()).thenReturn(ChallengeJoinStatus.JOINED);

        when(challenge.getCurrentRound()).thenReturn(currentRound);

        // startDate가 null이 아니어야 "라운드 기간 체크" 로직을 통과할 수 있습니다.
        when(currentRound.getStartDate()).thenReturn(today.minusDays(10));

        // 수정: endDate를 D-3 기간 안으로 설정
        when(currentRound.getEndDate()).thenReturn(today.plusDays(3));

        RoundDecisionRequestDto request = new RoundDecisionRequestDto(NextRoundIntent.UNDECIDED, null);

        // when & then
        assertThatThrownBy(() -> roundDecisionService.decideNextRound(userId, challengeId, request))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROUND_DECISION_INTENT_INVALID);

        verifyNoInteractions(roundRecordRepository);
    }

    @Test
    @DisplayName("decideNextRound: 결정 기간이 아니면 ROUND_DECISION_PERIOD_NOT_OPEN 예외")
    void decideNextRound_throws_whenOutsideDecisionWindow() {
        // given
        Long userId = 1L;
        Long challengeId = 10L;
        LocalDate today = LocalDate.now();

        Challenge challenge = mock(Challenge.class);
        Round currentRound = mock(Round.class);
        UserChallenge uc = mock(UserChallenge.class);

        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)).thenReturn(Optional.of(uc));
        when(uc.getStatus()).thenReturn(ChallengeJoinStatus.JOINED);

        when(challenge.getCurrentRound()).thenReturn(currentRound);

        // startDate 세팅
        when(currentRound.getStartDate()).thenReturn(today.minusDays(10));

        // 수정: D-3 기준으로 기간 밖 설정
        // end=today+5 -> open=today+2, close=today+3 (현재 today는 아직 오픈 전)
        when(currentRound.getEndDate()).thenReturn(today.plusDays(5));

        RoundDecisionRequestDto request = new RoundDecisionRequestDto(NextRoundIntent.CONTINUE, null);

        // when & then
        assertThatThrownBy(() -> roundDecisionService.decideNextRound(userId, challengeId, request))
                .isInstanceOf(GlobalException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ROUND_DECISION_PERIOD_NOT_OPEN);

        verifyNoInteractions(roundRecordRepository);
    }


    @Test
    @DisplayName("decideNextRound: 정상 요청이면 RoundRecord 업데이트, 이벤트 발행, 그리고 응답 DTO(isResponded=true) 반환")
    void decideNextRound_updatesRoundRecordIntent_andReturnsResponse() {
        // given
        Long userId = 1L;
        Long challengeId = 10L;
        LocalDate today = LocalDate.now();

        Challenge challenge = mock(Challenge.class);
        Round currentRound = mock(Round.class);
        UserChallenge uc = mock(UserChallenge.class);
        RoundRecord rr = mock(RoundRecord.class);
        when(challengeRepository.findById(challengeId)).thenReturn(Optional.of(challenge));
        when(userChallengeRepository.findByUserIdAndChallengeId(userId, challengeId)).thenReturn(Optional.of(uc));
        when(uc.getStatus()).thenReturn(ChallengeJoinStatus.JOINED);
        when(challenge.getCurrentRound()).thenReturn(currentRound);
        when(currentRound.getId()).thenReturn(100L);

        // 🔧 수정: D-3 기간 안으로 설정
        // endDate=today+3 -> open=today, close=today+1 -> today는 open에 해당
        when(currentRound.getStartDate()).thenReturn(today.minusDays(1));
        when(currentRound.getEndDate()).thenReturn(today.plusDays(3));

        when(roundRecordRepository.findByUserChallengeAndRoundId(uc, 100L)).thenReturn(Optional.of(rr));

        RoundDecisionRequestDto request = new RoundDecisionRequestDto(NextRoundIntent.CONTINUE, null);

        // when
        RoundDecisionResponseDto response = roundDecisionService.decideNextRound(userId, challengeId, request);

        // then
        // 1. DB 업데이트 확인
        verify(rr).updateNextRoundIntent(NextRoundIntent.CONTINUE);

        // 2. 응답 DTO 검증
        assertThat(response).isNotNull();
        assertThat(response.roundId()).isEqualTo(100L);
        assertThat(response.intent()).isEqualTo(NextRoundIntent.CONTINUE);
        assertThat(response.isResponded()).isTrue();
    }

    // -------------------------------------------------------------------------
    // 2) RoundDropServiceImpl 단위 테스트
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("dropNonContinuersAt: STOP/UNDECIDED인 사람만 DROPPED + 참가자 감소")
    void dropNonContinuersAt_dropsStopAndUndecided() {
        // given
        LocalDate endDate = LocalDate.now();

        Round endedRound = mock(Round.class);
        Challenge challenge = mock(Challenge.class);

        when(roundRepository.findAllByEndDate(endDate)).thenReturn(List.of(endedRound));
        when(endedRound.getId()).thenReturn(10L);
        when(endedRound.getChallenge()).thenReturn(challenge);

        when(challenge.getStatus()).thenReturn(ChallengeStatus.ONGOING);
        when(challenge.getId()).thenReturn(999L);

        // currentRound가 endedRound와 동일해야 처리됨(멱등성)
        when(challenge.getCurrentRound()).thenReturn(endedRound);

        RoundRecord rrContinue = mock(RoundRecord.class);
        when(rrContinue.getNextRoundIntent()).thenReturn(NextRoundIntent.CONTINUE);

        RoundRecord rrStop = mock(RoundRecord.class);
        when(rrStop.getNextRoundIntent()).thenReturn(NextRoundIntent.STOP);

        UserChallenge ucStop = mock(UserChallenge.class);
        when(rrStop.getUserChallenge()).thenReturn(ucStop);
        when(ucStop.getStatus()).thenReturn(ChallengeJoinStatus.JOINED);

        when(roundRecordRepository.findAllByRoundWithUserAndSetting(endedRound, ChallengeJoinStatus.JOINED))
                .thenReturn(List.of(rrContinue, rrStop));
        when(challengeRepository.decreaseCurrentParticipantCount(999L)).thenReturn(1);

        // when
        roundDropService.dropNonContinuersAt(endDate);

        // then
        verify(ucStop).updateStatus(ChallengeJoinStatus.DROPPED);
        verify(challengeRepository).decreaseCurrentParticipantCount(999L);

        // CONTINUE는 drop 대상이 아니므로 decrease 호출 추가 발생 X
        verify(challengeRepository, times(1)).decreaseCurrentParticipantCount(999L);
    }

    @Test
    @DisplayName("dropNonContinuersAt: 참가자 수 감소가 실패하면 하차 상태로 변경하지 않는다")
    void dropNonContinuersAt_doesNotDrop_whenParticipantCountCannotDecrease() {
        // given
        LocalDate endDate = LocalDate.now();

        Round endedRound = mock(Round.class);
        Challenge challenge = mock(Challenge.class);

        when(roundRepository.findAllByEndDate(endDate)).thenReturn(List.of(endedRound));
        when(endedRound.getId()).thenReturn(10L);
        when(endedRound.getChallenge()).thenReturn(challenge);

        when(challenge.getStatus()).thenReturn(ChallengeStatus.ONGOING);
        when(challenge.getId()).thenReturn(999L);
        when(challenge.getCurrentRound()).thenReturn(endedRound);

        RoundRecord rrStop = mock(RoundRecord.class);
        when(rrStop.getNextRoundIntent()).thenReturn(NextRoundIntent.STOP);

        UserChallenge ucStop = mock(UserChallenge.class);
        when(rrStop.getUserChallenge()).thenReturn(ucStop);
        when(ucStop.getStatus()).thenReturn(ChallengeJoinStatus.JOINED);

        when(roundRecordRepository.findAllByRoundWithUserAndSetting(endedRound, ChallengeJoinStatus.JOINED))
                .thenReturn(List.of(rrStop));
        when(challengeRepository.decreaseCurrentParticipantCount(999L)).thenReturn(0);

        // when
        roundDropService.dropNonContinuersAt(endDate);

        // then
        verify(challengeRepository).decreaseCurrentParticipantCount(999L);
        verify(ucStop, never()).updateStatus(ChallengeJoinStatus.DROPPED);
    }

    @Test
    @DisplayName("dropNonContinuersAt: currentRound가 이미 바뀐 경우 스킵(레코드 조회도 안 함)")
    void dropNonContinuersAt_skips_whenCurrentRoundChanged() {
        // given
        LocalDate endDate = LocalDate.now();

        Round endedRound = mock(Round.class);
        Round otherRound = mock(Round.class);
        Challenge challenge = mock(Challenge.class);

        when(roundRepository.findAllByEndDate(endDate)).thenReturn(List.of(endedRound));
        when(endedRound.getId()).thenReturn(10L);
        when(endedRound.getChallenge()).thenReturn(challenge);

        when(challenge.getStatus()).thenReturn(ChallengeStatus.ONGOING);
        when(challenge.getCurrentRound()).thenReturn(otherRound);
        when(otherRound.getId()).thenReturn(9999L); // endedRound(10L)과 다름

        // when
        roundDropService.dropNonContinuersAt(endDate);

        // then
        verify(roundRecordRepository, never()).findAllByRoundWithUserAndSetting(any(), any());
        verify(challengeRepository, never()).decreaseCurrentParticipantCount(any());
    }

    // -------------------------------------------------------------------------
    // 3) RoundLifecycleServiceImpl 단위 테스트
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("processRoundsEndedAt: CONTINUE가 없으면 챌린지를 FINISHED로 변경")
    void processRoundsEndedAt_finishes_whenNoContinuers() {
        // given
        mockTransaction(); // 트랜잭션 실행 모킹
        LocalDate endDate = LocalDate.now();
        Round endedRound = mock(Round.class);
        Challenge challenge = mock(Challenge.class);

        when(roundRepository.findAllByEndDate(endDate)).thenReturn(List.of(endedRound));
        when(endedRound.getId()).thenReturn(10L);
        // 트랜잭션 내부에서 호출하는 메서드 Stubbing
        when(roundRepository.findByIdWithChallengeAndCurrentRound(10L)).thenReturn(Optional.of(endedRound));

        when(endedRound.getChallenge()).thenReturn(challenge);
        when(challenge.getStatus()).thenReturn(ChallengeStatus.ONGOING);
        when(challenge.getCurrentRound()).thenReturn(endedRound);

        RoundRecord rrStop = mock(RoundRecord.class);
        when(rrStop.getNextRoundIntent()).thenReturn(NextRoundIntent.STOP);
        when(roundRecordRepository.findAllByRoundWithUserAndSetting(endedRound, ChallengeJoinStatus.JOINED))
                .thenReturn(List.of(rrStop));

        // when
        roundLifecycleService.processRoundsEndedAt(endDate);

        // then
        verify(challenge).updateStatus(ChallengeStatus.FINISHED);
    }

    @Test
    @DisplayName("processRoundsEndedAt: CONTINUE가 있으면 다음 라운드 생성 + RoundRecord 생성 + currentRound 교체")
    void processRoundsEndedAt_createsNextRound_andMovesCurrentRound() {
        // given
        mockTransaction();
        LocalDate endDate = LocalDate.now();
        Round endedRound = mock(Round.class);
        Challenge challenge = mock(Challenge.class);

        when(roundRepository.findAllByEndDate(endDate)).thenReturn(List.of(endedRound));
        when(endedRound.getId()).thenReturn(10L);
        when(roundRepository.findByIdWithChallengeAndCurrentRound(10L)).thenReturn(Optional.of(endedRound));

        when(endedRound.getRoundNumber()).thenReturn(1);
        when(endedRound.getChallenge()).thenReturn(challenge);
        when(challenge.getStatus()).thenReturn(ChallengeStatus.ONGOING);
        when(challenge.getId()).thenReturn(999L);
        when(challenge.getCurrentRound()).thenReturn(endedRound);

        RoundRecord rrContinue = mock(RoundRecord.class);
        when(rrContinue.getNextRoundIntent()).thenReturn(NextRoundIntent.CONTINUE);
        UserChallenge uc = mock(UserChallenge.class);
        when(rrContinue.getUserChallenge()).thenReturn(uc);
        when(roundRecordRepository.findAllByRoundWithUserAndSetting(endedRound, ChallengeJoinStatus.JOINED))
                .thenReturn(List.of(rrContinue));

        Round nextRound = mock(Round.class);
        when(nextRound.getRoundNumber()).thenReturn(2);
        when(roundRepository.findByChallengeIdAndRoundNumber(999L, 2)).thenReturn(Optional.empty());
        when(roundConverter.toNextRoundEntity(challenge, endedRound)).thenReturn(nextRound);
        when(roundRepository.save(nextRound)).thenReturn(nextRound);
        when(roundRecordRepository.existsByUserChallengeAndRound(uc, nextRound)).thenReturn(false);
        RoundRecord nextRR = mock(RoundRecord.class);
        when(roundConverter.toRoundRecordEntity(nextRound, uc)).thenReturn(nextRR);

        // when
        roundLifecycleService.processRoundsEndedAt(endDate);

        // then
        verify(roundRepository).save(nextRound);
        verify(roundRecordRepository).save(nextRR);
        verify(challenge).changeCurrentRound(nextRound);
    }

    @Test
    @DisplayName("processRoundsEndedAt: nextRound가 이미 있으면 save 없이 기존 라운드 사용")
    void processRoundsEndedAt_usesExistingNextRound() {
        // given
        mockTransaction();
        LocalDate endDate = LocalDate.now();
        Round endedRound = mock(Round.class);
        Challenge challenge = mock(Challenge.class);

        when(roundRepository.findAllByEndDate(endDate)).thenReturn(List.of(endedRound));
        when(endedRound.getId()).thenReturn(10L);
        when(roundRepository.findByIdWithChallengeAndCurrentRound(10L)).thenReturn(Optional.of(endedRound));

        when(endedRound.getRoundNumber()).thenReturn(1);
        when(endedRound.getChallenge()).thenReturn(challenge);
        when(challenge.getStatus()).thenReturn(ChallengeStatus.ONGOING);
        when(challenge.getId()).thenReturn(999L);
        when(challenge.getCurrentRound()).thenReturn(endedRound);

        RoundRecord rrContinue = mock(RoundRecord.class);
        when(rrContinue.getNextRoundIntent()).thenReturn(NextRoundIntent.CONTINUE);
        UserChallenge uc = mock(UserChallenge.class);
        when(rrContinue.getUserChallenge()).thenReturn(uc);
        when(roundRecordRepository.findAllByRoundWithUserAndSetting(endedRound, ChallengeJoinStatus.JOINED))
                .thenReturn(List.of(rrContinue));

        Round nextRound = mock(Round.class);
        when(roundRepository.findByChallengeIdAndRoundNumber(999L, 2)).thenReturn(Optional.of(nextRound));
        when(roundRecordRepository.existsByUserChallengeAndRound(uc, nextRound)).thenReturn(true);

        // when
        roundLifecycleService.processRoundsEndedAt(endDate);

        // then
        verify(roundRepository, never()).save(any());
        verify(challenge).changeCurrentRound(nextRound);
    }
}
