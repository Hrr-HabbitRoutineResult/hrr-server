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
	private WeakVerificationReportRepository weakReportRepository;
	@Mock
	private VerificationAbsenceLogRepository absenceLogRepository;
	@Mock
	private UserChallengeRepository userChallengeRepository;
	@Mock
	private ChallengeRepository challengeRepository;

	@Test
	@DisplayName("신고와 미인증 로그를 합산하여 경고 점수가 정확히 계산되는지 확인한다")
	void shouldCalculateWarnCountCorrectly() {
		// given
		Long recordId = 1L;
		Long challengeId = 100L;

		Challenge challenge = Challenge.builder().id(100L).currentParticipants(10).build();
		UserChallenge userChallenge = UserChallenge.builder().challenge(challenge).status(ChallengeJoinStatus.JOINED).build();
		RoundRecord roundRecord = RoundRecord.builder().id(recordId).userChallenge(userChallenge).warnCount(0).build();

		given(roundRecordRepository.findById(recordId)).willReturn(Optional.of(roundRecord));
		given(weakReportRepository.countByRoundRecordId(recordId)).willReturn(7L); // 7 / 3 = 2회 경고
		given(absenceLogRepository.countByRoundRecordId(recordId)).willReturn(1L);  // 1회 경고

		// 퇴출 로직 수행 시 필요한 Mock 설정
		given(challengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));
		given(userChallengeRepository.countByChallengeIdAndStatus(challengeId, ChallengeJoinStatus.JOINED)).willReturn(9L);

		// when
		roundRecordService.synchronizeWarnCount(recordId);

		// then: (7 / 3) + 1 = 3
		assertEquals(3, roundRecord.getWarnCount());
		assertEquals(ChallengeJoinStatus.KICKED, userChallenge.getStatus());
	}

	@Test
	@DisplayName("퇴출 발생 시 해당 챌린지의 참여 인원 재계산 로직이 호출된다")
	void shouldRecalculateParticipantsOnKickOut() {
		// given
		Long recordId = 1L;
		Long challengeId = 100L;
		Challenge challenge = Challenge.builder().id(challengeId).currentParticipants(10).build();
		UserChallenge userChallenge = UserChallenge.builder().challenge(challenge).status(ChallengeJoinStatus.JOINED).build();
		RoundRecord roundRecord = RoundRecord.builder().id(recordId).userChallenge(userChallenge).warnCount(0).build();

		given(roundRecordRepository.findById(recordId)).willReturn(Optional.of(roundRecord));
		given(weakReportRepository.countByRoundRecordId(recordId)).willReturn(0L);
		given(absenceLogRepository.countByRoundRecordId(recordId)).willReturn(3L); // 바로 퇴출 조건

		given(challengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));
		given(userChallengeRepository.countByChallengeIdAndStatus(challengeId, ChallengeJoinStatus.JOINED)).willReturn(5L);

		// when
		roundRecordService.synchronizeWarnCount(recordId);

		// then
		assertEquals(5, challenge.getCurrentParticipants()); // 인원이 5명으로 재설정되었는지 확인
		verify(userChallengeRepository, times(1)).countByChallengeIdAndStatus(any(), any());
	}
}
