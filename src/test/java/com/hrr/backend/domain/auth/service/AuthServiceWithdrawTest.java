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

        given(jwtService.extractUserId("test_token")).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(jwtService.getRemainingExpiration("test_token")).willReturn(Duration.ofMinutes(10));
        // when
        authService.withdraw(dummyToken);

        // then
        verify(user).withdraw();

        // 토큰 무효화 로직 수행 확인
        verify(jwtService).blacklistToken(eq("test_token"), any(Duration.class));
        verify(jwtService).deleteRefreshToken(userId);
    }

    @Test
    @DisplayName("회원 탈퇴 성공 - 참여 중인 챌린지가 없는 경우")
    void withdraw_success_no_active_challenges() {
        // given
        Long userId = 1L;
        String dummyToken = "Bearer test_token";
        User user = mock(User.class);

        given(jwtService.extractUserId("test_token")).willReturn(userId);
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        // 남은 유효기간이 0이면 블랙리스트 등록을 생략해야 함
        given(jwtService.getRemainingExpiration("test_token")).willReturn(Duration.ZERO);

        // when
        authService.withdraw(dummyToken);

        // then
        verify(user).withdraw();
        // 만료된 토큰은 블랙리스트 등록 불필요
        verify(jwtService, never()).blacklistToken(anyString(), any(Duration.class));
        verify(jwtService).deleteRefreshToken(userId);
    }
}