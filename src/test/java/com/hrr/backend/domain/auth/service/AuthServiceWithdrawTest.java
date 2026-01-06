package com.hrr.backend.domain.auth.service;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceWithdrawTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserChallengeRepository userChallengeRepository;

    @Test
    @DisplayName("회원 탈퇴 성공 - 참여 중인 챌린지 인원 감소 및 상태 변경 확인")
    void withdraw_success() {
        // given
        Long userId = 1L;
        User user = mock(User.class); // 유저 객체 모킹

        // 테스트용 챌린지 생성 (현재 인원 5명)
        Challenge challenge = Challenge.builder()
                .currentParticipants(5)
                .build();

        // 테스트용 유저-챌린지 참여 정보 생성 (JOINED 상태)
        UserChallenge userChallenge = UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .status(ChallengeJoinStatus.JOINED)
                .build();

        // Repository 모킹
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userChallengeRepository.findByUserAndStatus(user, ChallengeJoinStatus.JOINED))
                .willReturn(List.of(userChallenge));

        // when
        authService.withdraw(userId);

        // then
        // 1. 유저 엔티티의 withdraw() 메서드가 호출되었는지 확인
        verify(user).withdraw();

        // 2. 챌린지 엔티티의 인원수가 감소했는지 확인 (5 -> 4)
        assertThat(challenge.getCurrentParticipants()).isEqualTo(4);

        // 3. UserChallenge의 상태가 DROPPED로 변경되었는지 확인
        assertThat(userChallenge.getStatus()).isEqualTo(ChallengeJoinStatus.DROPPED);
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 참여 중인 챌린지가 없는 경우")
    void withdraw_success_no_active_challenges() {
        // given
        Long userId = 1L;
        User user = mock(User.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        // 참여 중인 챌린지가 없음
        given(userChallengeRepository.findByUserAndStatus(user, ChallengeJoinStatus.JOINED))
                .willReturn(List.of());

        // when
        authService.withdraw(userId);

        // then
        verify(user).withdraw();
        // 별다른 에러 없이 로직이 종료되어야 함
    }
}