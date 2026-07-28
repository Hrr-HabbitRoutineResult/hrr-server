package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.converter.ChallengeConverter;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.notification.event.ChallengeVacancyEvent;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceLeaveTest {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private RoundRecordRepository roundRecordRepository;
    @Mock private ChallengeConverter challengeConverter;
    @Mock private ChallengeStaticsService challengeStaticsService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    @Test
    @DisplayName("정원이 꽉 찬 시작 전 챌린지에서 참여자가 나가면 CANCELLED 처리 후 빈자리 이벤트를 발행한다")
    void leaveChallenge_PublishesVacancyEvent_WhenFullChallengeBecomesVacant() {
        User user = User.builder().name("user").nickname("user_nick").build();
        Challenge challenge = createChallenge(30, 30);
        UserChallenge userChallenge = UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .role(UserChallengeRole.CHALLENGER)
                .status(ChallengeJoinStatus.JOINED)
                .build();

        given(challengeRepository.findByIdForUpdate(1L)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.of(userChallenge));
        given(roundRecordRepository.findByUserChallengeAndRoundId(userChallenge, 10L)).willReturn(Optional.empty());
        given(challengeConverter.toLeaveResponseDto(challenge))
                .willReturn(new ChallengeResponseDto.LeaveChallengeDto(1L, 29));

        challengeService.leaveChallenge(user, 1L);

        assertThat(userChallenge.getStatus()).isEqualTo(ChallengeJoinStatus.CANCELLED);
        assertThat(challenge.getCurrentParticipants()).isEqualTo(29);

        ArgumentCaptor<ChallengeVacancyEvent> captor = ArgumentCaptor.forClass(ChallengeVacancyEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().challengeId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("이미 빈자리가 있던 챌린지에서 참여자가 나가면 빈자리 이벤트를 발행하지 않는다")
    void leaveChallenge_DoesNotPublishVacancyEvent_WhenChallengeWasNotFull() {
        User user = User.builder().name("user").nickname("user_nick").build();
        Challenge challenge = createChallenge(30, 28);
        UserChallenge userChallenge = UserChallenge.builder()
                .user(user)
                .challenge(challenge)
                .role(UserChallengeRole.CHALLENGER)
                .status(ChallengeJoinStatus.JOINED)
                .build();

        given(challengeRepository.findByIdForUpdate(1L)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.of(userChallenge));
        given(roundRecordRepository.findByUserChallengeAndRoundId(userChallenge, 10L)).willReturn(Optional.empty());
        given(challengeConverter.toLeaveResponseDto(challenge))
                .willReturn(new ChallengeResponseDto.LeaveChallengeDto(1L, 27));

        challengeService.leaveChallenge(user, 1L);

        assertThat(userChallenge.getStatus()).isEqualTo(ChallengeJoinStatus.CANCELLED);
        assertThat(challenge.getCurrentParticipants()).isEqualTo(27);
        verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any(ChallengeVacancyEvent.class));
    }

    private Challenge createChallenge(int maxParticipants, int currentParticipants) {
        Round round = Round.builder()
                .id(10L)
                .roundNumber(1)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusWeeks(3))
                .build();

        Challenge challenge = Challenge.builder()
                .id(1L)
                .title("테스트 챌린지")
                .description("설명")
                .status(ChallengeStatus.UPCOMING)
                .category(Category.HEALTH)
                .startDate(LocalDate.now().plusDays(1).atStartOfDay())
                .verificationType(VerificationType.TEXT)
                .verifyStartTime(LocalTime.of(9, 0))
                .verifyEndTime(LocalTime.of(22, 0))
                .currentParticipants(currentParticipants)
                .maxParticipants(maxParticipants)
                .isPublic(true)
                .isViewerMode(false)
                .currentRound(round)
                .build();
        return challenge;
    }
}
