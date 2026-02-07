package com.hrr.backend.domain.auth.service;

import static org.mockito.BDDMockito.*;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration; // import 추가
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

    // [추가] withdraw 메서드 내부에서 jwtService를 호출하므로 Mock 추가 필요
    @Mock
    private JwtService jwtService;

    @Test
    @DisplayName("회원 탈퇴 성공 - 참여 중인 챌린지 인원 감소 및 상태 변경 확인")
    void withdraw_success() {
        // given
        Long userId = 1L;
        String dummyToken = "Bearer test_token"; // 더미 토큰
        User user = mock(User.class);

        Challenge challenge = Challenge.builder()
                .currentParticipants(5)
                .build();

        UserChallenge userChallenge = UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .status(ChallengeJoinStatus.JOINED)
                .build();

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userChallengeRepository.findByUserAndStatus(user, ChallengeJoinStatus.JOINED))
                .willReturn(List.of(userChallenge));

        // [추가] JwtService 호출에 대한 Stubbing (NPE 방지)
        given(jwtService.getRemainingExpiration(anyString())).willReturn(Duration.ofMinutes(10));

        // when
        // [수정] 인자 2개 전달 (userId, token)
        authService.withdraw(userId, dummyToken);

        // then
        verify(user).withdraw();

        // 챌린지 로직이 정상 수행되었는지 확인 (AssertionFailedError 해결 확인용)
        assertThat(challenge.getCurrentParticipants()).isEqualTo(4);
        assertThat(userChallenge.getStatus()).isEqualTo(ChallengeJoinStatus.DROPPED);

        // 토큰 삭제 로직 수행 확인
        verify(jwtService).blacklistToken(anyString(), any(Duration.class));
        verify(jwtService).deleteRefreshToken(userId);
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 참여 중인 챌린지가 없는 경우")
    void withdraw_success_no_active_challenges() {
        // given
        Long userId = 1L;
        String dummyToken = "Bearer test_token";
        User user = mock(User.class);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(userChallengeRepository.findByUserAndStatus(user, ChallengeJoinStatus.JOINED))
                .willReturn(List.of()); // 빈 리스트 반환

        given(jwtService.getRemainingExpiration(anyString())).willReturn(Duration.ofMinutes(10));

        // when
        // [수정] 인자 2개 전달
        authService.withdraw(userId, dummyToken);

        // then
        verify(user).withdraw();
        verify(jwtService).deleteRefreshToken(userId);

        // UnnecessaryStubbingException 해결:
        // AuthService에 userChallengeRepository 호출 로직이 복구되었으므로,
        // 위에서 선언한 given(userChallengeRepository...)가 정상적으로 사용되어 에러가 사라집니다.
    }
}