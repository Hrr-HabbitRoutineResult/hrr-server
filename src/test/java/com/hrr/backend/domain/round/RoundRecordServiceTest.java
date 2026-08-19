package com.hrr.backend.domain.round;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.report.repository.WeakVerificationReportRepository;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.service.RoundRecordServiceImpl;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.verification.repository.VerificationAbsenceLogRepository;

@ExtendWith(MockitoExtension.class)
class RoundRecordServiceTest {

	@InjectMocks
	private RoundRecordServiceImpl roundRecordService;

	@Mock
	private RoundRecordRepository roundRecordRepository;
    @Mock
    private WeakVerificationReportRepository weakVerificationReportRepository;
    @Mock
    private VerificationAbsenceLogRepository verificationAbsenceLogRepository;
	@Mock
	private UserChallengeRepository userChallengeRepository;
    @Mock
    private ChallengeRepository challengeRepository;

    @Test
    @DisplayName("부실 인증 신고 3건당 경고 1회로 계산된다")
    void shouldCalculateWarnCountFromWeakReportsOnly() {
        // given
        Long recordId = 1L;
        RoundRecord roundRecord = RoundRecord.builder().id(recordId).warnCount(0).build();

        given(roundRecordRepository.findByIdWithPessimisticLock(recordId)).willReturn(Optional.of(roundRecord));
        given(weakVerificationReportRepository.countByRoundRecordId(recordId)).willReturn(7L);

        // when
        roundRecordService.synchronizeWarnCount(recordId);

        // then: 7 / 3 = 2
        assertEquals(2, roundRecord.getWarnCount());
    }

    @Test
    @DisplayName("미인증 로그는 경고 횟수에 더 이상 반영되지 않는다")
    void shouldNotCountAbsenceLogIntoWarnCount() {
        // given
        Long recordId = 1L;
        RoundRecord roundRecord = RoundRecord.builder().id(recordId).warnCount(0).build();

        given(roundRecordRepository.findByIdWithPessimisticLock(recordId)).willReturn(Optional.of(roundRecord));
        given(weakVerificationReportRepository.countByRoundRecordId(recordId)).willReturn(3L);

        // when
        roundRecordService.synchronizeWarnCount(recordId);

        // then: 부실 신고 3건 -> 경고 1회. 미인증 로그가 아무리 쌓여도 값이 변하지 않아야 한다.
        assertEquals(1, roundRecord.getWarnCount());
        // 미인증 로그 조회 자체가 일어나지 않는다 (계산식에서 제거됨)
        verify(verificationAbsenceLogRepository, never()).countByRoundRecordId(anyLong());
    }

    @Test
    @DisplayName("부실 인증 신고가 3건 미만이면 경고는 0회다")
    void shouldNotEarnWarnCountUnderThreeReports() {
        // given
        Long recordId = 1L;
        RoundRecord roundRecord = RoundRecord.builder().id(recordId).warnCount(0).build();

        given(roundRecordRepository.findByIdWithPessimisticLock(recordId)).willReturn(Optional.of(roundRecord));
        given(weakVerificationReportRepository.countByRoundRecordId(recordId)).willReturn(2L);

        // when
        roundRecordService.synchronizeWarnCount(recordId);

        // then
        assertEquals(0, roundRecord.getWarnCount());
    }

    @Test
    @DisplayName("경고가 누적되어도 참여 상태가 KICKED로 자동 전환되지 않는다")
    void shouldNotKickOutOnWarnCountAccumulation() {
        // given
        Long recordId = 1L;
        Challenge challenge = Challenge.builder().id(100L).currentParticipants(10).build();
        UserChallenge userChallenge = UserChallenge.builder()
                .challenge(challenge)
                .status(ChallengeJoinStatus.JOINED)
                .build();
        RoundRecord roundRecord = RoundRecord.builder()
                .id(recordId)
                .userChallenge(userChallenge)
                .warnCount(0)
                .build();

        given(roundRecordRepository.findByIdWithPessimisticLock(recordId)).willReturn(Optional.of(roundRecord));
        given(weakVerificationReportRepository.countByRoundRecordId(recordId)).willReturn(9L); // 경고 3회

        // when
        roundRecordService.synchronizeWarnCount(recordId);

        // then: 퇴출 기능이 폐지되어 경고 3회에도 JOINED가 유지되고 인원 재계산도 일어나지 않는다
        assertEquals(3, roundRecord.getWarnCount());
        assertEquals(ChallengeJoinStatus.JOINED, userChallenge.getStatus());
        assertEquals(10, challenge.getCurrentParticipants());
        verify(userChallengeRepository, never()).countByChallengeIdAndStatus(any(), any());
    }
}