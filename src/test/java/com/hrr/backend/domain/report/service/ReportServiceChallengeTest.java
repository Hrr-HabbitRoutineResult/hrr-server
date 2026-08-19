package com.hrr.backend.domain.report.service;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.report.dto.ReportRequestDto;
import com.hrr.backend.domain.report.entity.ChallengeReport;
import com.hrr.backend.domain.report.repository.ChallengeReportRepository;
import com.hrr.backend.domain.report.repository.UserReportRepository;
import com.hrr.backend.domain.report.repository.VerificationPostReportRepository;
import com.hrr.backend.domain.report.repository.WeakVerificationReportRepository;
import com.hrr.backend.domain.round.service.RoundRecordService;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.common.enums.ReportReason;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportServiceChallengeTest {

    @InjectMocks
    private ReportServiceImpl reportService;

    @Mock private VerificationRepository verificationRepository;
    @Mock private VerificationPostReportRepository verificationPostReportRepository;
    @Mock private ChallengeRepository challengeRepository;
    @Mock private ChallengeReportRepository challengeReportRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserReportRepository userReportRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private WeakVerificationReportRepository weakVerificationReportRepository;
    @Mock private RoundRecordService roundRecordService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    void reportChallenge_joinedChallenger_savesReport() {
        User reporter = mock(User.class);
        Challenge challenge = mock(Challenge.class);
        UserChallenge participation = mock(UserChallenge.class);
        ReportRequestDto request = request(1L, ReportReason.OTHER, "상세 사유");

        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByUserAndChallenge(reporter, challenge))
                .willReturn(Optional.of(participation));
        given(participation.getStatus()).willReturn(ChallengeJoinStatus.JOINED);
        given(participation.getRole()).willReturn(UserChallengeRole.CHALLENGER);
        given(challengeReportRepository.existsByReporterAndChallenge(reporter, challenge)).willReturn(false);

        reportService.reportChallenge(reporter, request);

        ArgumentCaptor<ChallengeReport> captor = ArgumentCaptor.forClass(ChallengeReport.class);
        verify(challengeReportRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getReporter()).isEqualTo(reporter);
        assertThat(captor.getValue().getChallenge()).isEqualTo(challenge);
        assertThat(captor.getValue().getReason()).isEqualTo(ReportReason.OTHER);
        assertThat(captor.getValue().getDescription()).isEqualTo("상세 사유");
    }

    @Test
    void reportChallenge_nonParticipant_throwsParticipantOnly() {
        User reporter = mock(User.class);
        Challenge challenge = mock(Challenge.class);
        ReportRequestDto request = request(1L, ReportReason.SPAM_OR_SCAM, null);

        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByUserAndChallenge(reporter, challenge)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.reportChallenge(reporter, request))
                .isInstanceOfSatisfying(GlobalException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHALLENGE_REPORT_PARTICIPANT_ONLY));
        verify(challengeReportRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportChallenge_predefinedReason_doesNotStoreDescription() {
        User reporter = mock(User.class);
        Challenge challenge = mock(Challenge.class);
        UserChallenge participation = mock(UserChallenge.class);
        ReportRequestDto request = request(1L, ReportReason.SPAM_OR_SCAM, "저장되면 안 되는 값");

        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByUserAndChallenge(reporter, challenge))
                .willReturn(Optional.of(participation));
        given(participation.getStatus()).willReturn(ChallengeJoinStatus.JOINED);
        given(participation.getRole()).willReturn(UserChallengeRole.CHALLENGER);
        given(challengeReportRepository.existsByReporterAndChallenge(reporter, challenge)).willReturn(false);

        reportService.reportChallenge(reporter, request);

        ArgumentCaptor<ChallengeReport> captor = ArgumentCaptor.forClass(ChallengeReport.class);
        verify(challengeReportRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(ReportReason.SPAM_OR_SCAM);
        assertThat(captor.getValue().getDescription()).isNull();
    }

    @Test
    void reportChallenge_owner_throwsOwnerNotAllowed() {
        User reporter = mock(User.class);
        Challenge challenge = mock(Challenge.class);
        UserChallenge participation = mock(UserChallenge.class);
        ReportRequestDto request = request(1L, ReportReason.SPAM_OR_SCAM, null);

        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByUserAndChallenge(reporter, challenge))
                .willReturn(Optional.of(participation));
        given(participation.getStatus()).willReturn(ChallengeJoinStatus.JOINED);
        given(participation.getRole()).willReturn(UserChallengeRole.OWNER);

        assertThatThrownBy(() -> reportService.reportChallenge(reporter, request))
                .isInstanceOfSatisfying(GlobalException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHALLENGE_REPORT_OWNER_NOT_ALLOWED));
        verify(challengeReportRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void reportChallenge_duplicate_throwsAlreadyReported() {
        User reporter = mock(User.class);
        Challenge challenge = mock(Challenge.class);
        UserChallenge participation = mock(UserChallenge.class);
        ReportRequestDto request = request(1L, ReportReason.SPAM_OR_SCAM, null);

        given(challengeRepository.findById(1L)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByUserAndChallenge(reporter, challenge))
                .willReturn(Optional.of(participation));
        given(participation.getStatus()).willReturn(ChallengeJoinStatus.JOINED);
        given(participation.getRole()).willReturn(UserChallengeRole.CHALLENGER);
        given(challengeReportRepository.existsByReporterAndChallenge(reporter, challenge)).willReturn(true);

        assertThatThrownBy(() -> reportService.reportChallenge(reporter, request))
                .isInstanceOfSatisfying(GlobalException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.CHALLENGE_ALREADY_REPORTED));
        verify(challengeReportRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    private ReportRequestDto request(Long targetId, ReportReason reason, String description) {
        ReportRequestDto request = mock(ReportRequestDto.class);
        lenient().when(request.getTargetId()).thenReturn(targetId);
        lenient().when(request.getReason()).thenReturn(reason);
        lenient().when(request.getDescription()).thenReturn(description);
        return request;
    }
}
