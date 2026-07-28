package com.hrr.backend.domain.round;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.notification.entity.NotificationSetting;
import com.hrr.backend.domain.notification.repository.NotificationSettingRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.entity.enums.NextRoundIntent;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.round.service.RoundDropProcessor;
import com.hrr.backend.domain.round.service.RoundDropService;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import com.hrr.backend.global.exception.GlobalException;

@SpringBootTest
class RoundDropTransactionIntegrationTest {

    @Autowired private RoundDropProcessor roundDropProcessor;
    @Autowired private RoundDropService roundDropService;
    @Autowired private UserRepository userRepository;
    @MockitoSpyBean private ChallengeRepository challengeRepository;
    @Autowired private UserChallengeRepository userChallengeRepository;
    @Autowired private RoundRepository roundRepository;
    @Autowired private RoundRecordRepository roundRecordRepository;
    @Autowired private NotificationSettingRepository notificationSettingRepository;

    @Test
    @DisplayName("processRound: 중간 실패 시 같은 Round의 드랍 처리를 모두 롤백하고 다른 Round는 계속 처리한다")
    void processRound_rollsBackSingleRoundAndDropServiceContinues() {
        // given
        LocalDate endDate = LocalDate.now();
        Round failedRound = createRoundWithJoinedUsers("failed", endDate, 2);
        Round successRound = createRoundWithJoinedUsers("success", endDate, 1);

        doReturn(1, 0).when(challengeRepository)
                .decreaseCurrentParticipantCount(failedRound.getChallenge().getId());
        doReturn(1).when(challengeRepository)
                .decreaseCurrentParticipantCount(successRound.getChallenge().getId());

        // when & then
        assertThatThrownBy(() -> roundDropProcessor.processRound(failedRound))
                .isInstanceOf(GlobalException.class);

        List<UserChallenge> failedParticipants = userChallengeRepository.findAllByChallengeId(
                failedRound.getChallenge().getId()
        );
        assertThat(failedParticipants)
                .hasSize(2)
                .allSatisfy(uc -> assertThat(uc.getStatus()).isEqualTo(ChallengeJoinStatus.JOINED));

        // when
        roundDropService.dropNonContinuersAt(endDate);

        // then
        failedParticipants = userChallengeRepository.findAllByChallengeId(failedRound.getChallenge().getId());
        List<UserChallenge> successParticipants = userChallengeRepository.findAllByChallengeId(
                successRound.getChallenge().getId()
        );

        assertThat(failedParticipants)
                .hasSize(2)
                .allSatisfy(uc -> assertThat(uc.getStatus()).isEqualTo(ChallengeJoinStatus.JOINED));
        assertThat(successParticipants)
                .hasSize(1)
                .allSatisfy(uc -> assertThat(uc.getStatus()).isEqualTo(ChallengeJoinStatus.DROPPED));
    }

    private Round createRoundWithJoinedUsers(String prefix, LocalDate endDate, int joinedUserCount) {
        Challenge challenge = challengeRepository.save(Challenge.builder()
                .title(prefix + " challenge")
                .description("test challenge")
                .isPublic(true)
                .category(Category.STUDY)
                .isViewerMode(false)
                .maxParticipants(10)
                .currentParticipants(joinedUserCount)
                .startDate(LocalDateTime.now().minusDays(7))
                .verificationType(VerificationType.TEXT)
                .verifyStartTime(LocalTime.of(0, 0))
                .verifyEndTime(LocalTime.of(23, 59))
                .status(ChallengeStatus.ONGOING)
                .build());

        Round round = roundRepository.save(Round.builder()
                .challenge(challenge)
                .roundNumber(1)
                .startDate(endDate.minusWeeks(3).plusDays(1))
                .endDate(endDate)
                .build());
        challenge.changeCurrentRound(round);
        challengeRepository.save(challenge);

        for (int i = 0; i < joinedUserCount; i++) {
            User user = userRepository.save(User.builder()
                    .nickname(prefix + "-user-" + i)
                    .isPublic(true)
                    .build());
            notificationSettingRepository.save(NotificationSetting.builder()
                    .user(user)
                    .build());
            UserChallenge userChallenge = userChallengeRepository.save(UserChallenge.builder()
                    .user(user)
                    .challenge(challenge)
                    .status(ChallengeJoinStatus.JOINED)
                    .build());
            roundRecordRepository.save(RoundRecord.builder()
                    .round(round)
                    .userChallenge(userChallenge)
                    .nextRoundIntent(NextRoundIntent.STOP)
                    .build());
        }

        return round;
    }
}
