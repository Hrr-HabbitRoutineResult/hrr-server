package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.converter.ChallengeConverter;
import com.hrr.backend.domain.challenge.dto.ChallengeRequestDto;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.point.service.PointService;
import com.hrr.backend.domain.round.converter.RoundConverter;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.user.converter.UserChallengeConverter;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceJoinTest {

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private RoundRecordRepository roundRecordRepository;
    @Mock private UserChallengeConverter userChallengeConverter;
    @Mock private ChallengeConverter challengeConverter;
    @Mock private RoundConverter roundConverter;
    @Mock private ChallengeStaticsService challengeStaticsService;
    @Mock private PointService pointService;

    private User user;
    private Challenge challenge;
    private ChallengeRequestDto.JoinChallengeDto joinReq;

    @BeforeEach
    void setUp() {
        user = mock(User.class);
        challenge = mock(Challenge.class);
        joinReq = new ChallengeRequestDto.JoinChallengeDto();

        // 기본 챌린지 설정 (진행 중, 정원 여유, 공개 챌린지)
        lenient().when(challenge.getStatus()).thenReturn(ChallengeStatus.ONGOING);
        lenient().when(challenge.getCurrentParticipants()).thenReturn(5);
        lenient().when(challenge.getMaxParticipants()).thenReturn(10);
        lenient().when(challenge.getCurrentRound()).thenReturn(mock(Round.class));
        lenient().when(challenge.getIsPublic()).thenReturn(true);
    }

    @Test
    @DisplayName("가입 상황 1: 신규 유저 참여 -> save() 호출 확인 및 가입 성공")
    void join_NewUser_Success() {
        // [Given]
        Long challengeId = 1L;
        given(challengeRepository.findByIdForUpdate(anyLong())).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.empty());

        UserChallenge newUc = mock(UserChallenge.class);
        given(userChallengeConverter.toChallenger(user, challenge)).willReturn(newUc);
        given(challengeConverter.toJoinResponseDto(challenge)).willReturn(mock(ChallengeResponseDto.JoinChallengeDto.class));

        // [When]
        challengeService.joinChallenge(user, challengeId, joinReq);

        // [Then]
        verify(userChallengeRepository, times(1)).save(any(UserChallenge.class));
        verify(challenge, times(1)).increaseCurrentParticipants();
    }

    @Test
    @DisplayName("가입 상황 2: 하차(DROPPED) 유저 재참여 -> updateStatus() 호출 및 save() 미호출 (DB 충돌 방지)")
    void join_DroppedUser_Success() {
        // [Given]
        Long challengeId = 1L;
        given(challengeRepository.findByIdForUpdate(anyLong())).willReturn(Optional.of(challenge));

        UserChallenge existingUc = mock(UserChallenge.class);
        given(existingUc.getStatus()).willReturn(ChallengeJoinStatus.DROPPED);
        given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.of(existingUc));

        given(challengeConverter.toJoinResponseDto(challenge)).willReturn(mock(ChallengeResponseDto.JoinChallengeDto.class));

        // [When]
        challengeService.joinChallenge(user, challengeId, joinReq);

        // [Then]
        verify(existingUc, times(1)).updateStatus(ChallengeJoinStatus.JOINED);
        verify(userChallengeRepository, never()).save(any(UserChallenge.class));
    }

    @Test
    @DisplayName("가입 상황 2-1: 시작 전 나간(CANCELLED) 유저 재참여 -> updateStatus() 호출 및 save() 미호출")
    void join_CancelledUser_Success() {
        // [Given]
        Long challengeId = 1L;
        given(challengeRepository.findByIdForUpdate(anyLong())).willReturn(Optional.of(challenge));

        UserChallenge existingUc = mock(UserChallenge.class);
        given(existingUc.getStatus()).willReturn(ChallengeJoinStatus.CANCELLED);
        given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.of(existingUc));

        given(challengeConverter.toJoinResponseDto(challenge)).willReturn(mock(ChallengeResponseDto.JoinChallengeDto.class));

        // [When]
        challengeService.joinChallenge(user, challengeId, joinReq);

        // [Then]
        verify(existingUc, times(1)).updateStatus(ChallengeJoinStatus.JOINED);
        verify(userChallengeRepository, never()).save(any(UserChallenge.class));
    }

    @Test
    @DisplayName("가입 상황 3: 강퇴(KICKED) 유저 재참여 시도 -> CHALLENGE_KICKED_USER 예외 발생")
    void join_KickedUser_Throws_Exception() {
        // [Given]
        Long challengeId = 1L;
        given(challengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));

        UserChallenge kickedUc = mock(UserChallenge.class);
        given(kickedUc.getStatus()).willReturn(ChallengeJoinStatus.KICKED);
        given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.of(kickedUc));

        // [When & Then]
        GlobalException exception = assertThrows(GlobalException.class, () ->
                challengeService.joinChallenge(user, challengeId, joinReq)
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHALLENGE_KICKED_USER);
    }

    @Test
    @DisplayName("가입 상황 4: 이미 참여 중(JOINED)인 유저 -> CHALLENGE_ALREADY_JOINED 예외 발생")
    void join_AlreadyJoinedUser_Throws_Exception() {
        // [Given]
        Long challengeId = 1L;
        given(challengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));

        UserChallenge joinedUc = mock(UserChallenge.class);
        given(joinedUc.getStatus()).willReturn(ChallengeJoinStatus.JOINED);
        given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.of(joinedUc));

        // [When & Then]
        GlobalException exception = assertThrows(GlobalException.class, () ->
                challengeService.joinChallenge(user, challengeId, joinReq)
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHALLENGE_ALREADY_JOINED);
    }

    @Test
    @DisplayName("가입 상황 5: 참여 중인 챌린지가 5개 초과 -> MAX_CHALLENGE_EXCEEDED 예외 발생")
    void join_MaxLimitExceeded_Throws_Exception() {
        // [Given]
        Long challengeId = 1L;
        given(challengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));
        given(challengeRepository.countByUserIdAndStatus(any(), eq(ChallengeJoinStatus.JOINED))).willReturn(5L);

        // [When & Then]
        GlobalException exception = assertThrows(GlobalException.class, () ->
                challengeService.joinChallenge(user, challengeId, joinReq)
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MAX_CHALLENGE_EXCEEDED);
    }

    @Test
    @DisplayName("가입 상황 6: 챌린지 정원 마감 -> CHALLENGE_FULL 예외 발생")
    void join_ChallengeFull_Throws_Exception() {
        // [Given]
        Long challengeId = 1L;
        given(challengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));
        given(challenge.getCurrentParticipants()).willReturn(10);
        given(challenge.getMaxParticipants()).willReturn(10);

        // [When & Then]
        GlobalException exception = assertThrows(GlobalException.class, () ->
                challengeService.joinChallenge(user, challengeId, joinReq)
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHALLENGE_FULL);
    }

    @Test
    @DisplayName("가입 상황 7: 비공개 챌린지 비밀번호 불일치 -> CHALLENGE_PASSWORD_MISMATCH 예외 발생")
    void join_PasswordMismatch_Throws_Exception() {
        // [Given]
        Long challengeId = 1L;
        given(challengeRepository.findByIdForUpdate(challengeId)).willReturn(Optional.of(challenge));

        given(challenge.getIsPublic()).willReturn(false);
        given(challenge.getPassword()).willReturn("1234");

        joinReq.setPassword("9999");

        // [When & Then]
        GlobalException exception = assertThrows(GlobalException.class, () ->
                challengeService.joinChallenge(user, challengeId, joinReq)
        );
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CHALLENGE_PASSWORD_MISMATCH); //
    }
}
