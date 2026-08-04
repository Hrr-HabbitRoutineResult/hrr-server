package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeWait;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeWaitRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceWaitTest {

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private ChallengeWaitRepository challengeWaitRepository;

    private User user;
    private Challenge challenge;

    @BeforeEach
    void setUp() {
        user = org.mockito.Mockito.mock(User.class);
        challenge = org.mockito.Mockito.mock(Challenge.class);

        lenient().when(user.getId()).thenReturn(10L);
    }

    @Test
    @DisplayName("빈자리 알림 신청: 최대 참여 개수 미도달 + 만석이면 신청 성공")
    void registerChallengeWait_underMaxLimitAndFull_savesWait() {
        Long challengeId = 1L;

        given(challengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.existsByUserAndChallengeAndStatus(user, challenge, ChallengeJoinStatus.JOINED))
                .willReturn(false);
        given(challengeRepository.countByUserIdAndStatus(10L, ChallengeJoinStatus.JOINED)).willReturn(4L);
        given(challengeWaitRepository.existsByUserAndChallenge(user, challenge)).willReturn(false);
        given(challenge.getCurrentParticipants()).willReturn(10);
        given(challenge.getMaxParticipants()).willReturn(10);

        challengeService.registerChallengeWait(user, challengeId);

        ArgumentCaptor<ChallengeWait> captor = ArgumentCaptor.forClass(ChallengeWait.class);
        verify(challengeWaitRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(captor.getValue().getChallenge()).isEqualTo(challenge);
    }

    @Test
    @DisplayName("빈자리 알림 신청: 최대 참여 개수 도달 + 만석이면 MAX_CHALLENGE_EXCEEDED 예외")
    void registerChallengeWait_atMaxLimitAndFull_throwsMaxChallengeExceeded() {
        Long challengeId = 1L;

        given(challengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.existsByUserAndChallengeAndStatus(user, challenge, ChallengeJoinStatus.JOINED))
                .willReturn(false);
        given(challengeRepository.countByUserIdAndStatus(10L, ChallengeJoinStatus.JOINED)).willReturn(5L);

        GlobalException exception = assertThrows(GlobalException.class,
                () -> challengeService.registerChallengeWait(user, challengeId));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MAX_CHALLENGE_EXCEEDED);
        verify(challengeWaitRepository, never()).save(org.mockito.Mockito.any(ChallengeWait.class));
    }

    @Test
    @DisplayName("빈자리 알림 신청: 참여 개수 제한 검증 실패 시 ChallengeWait가 저장되지 않는다")
    void registerChallengeWait_limitValidationFails_doesNotSaveWait() {
        Long challengeId = 1L;

        given(challengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.existsByUserAndChallengeAndStatus(user, challenge, ChallengeJoinStatus.JOINED))
                .willReturn(false);
        given(challengeRepository.countByUserIdAndStatus(10L, ChallengeJoinStatus.JOINED)).willReturn(5L);

        assertThrows(GlobalException.class, () -> challengeService.registerChallengeWait(user, challengeId));

        verify(challengeWaitRepository, never()).save(org.mockito.Mockito.any(ChallengeWait.class));
        verify(challengeWaitRepository).existsByUserAndChallenge(user, challenge);
    }

    @Test
    @DisplayName("빈자리 알림 신청: 이미 신청한 사용자가 최대 참여 개수 도달 후 재요청하면 중복 신청 예외")
    void registerChallengeWait_alreadyWaitedAndAtMaxLimit_throwsAlreadyExistWithoutSaving() {
        Long challengeId = 1L;

        given(challengeRepository.findById(challengeId)).willReturn(Optional.of(challenge));
        given(challengeWaitRepository.existsByUserAndChallenge(user, challenge)).willReturn(true);

        GlobalException exception = assertThrows(GlobalException.class,
                () -> challengeService.registerChallengeWait(user, challengeId));

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHALLENGE_WAIT_ALREADY_EXIST);
        verify(challengeRepository, never()).countByUserIdAndStatus(10L, ChallengeJoinStatus.JOINED);
        verify(challengeWaitRepository, never()).save(org.mockito.Mockito.any(ChallengeWait.class));
    }
}
