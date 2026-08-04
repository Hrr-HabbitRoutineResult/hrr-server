package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.converter.ChallengeConverter;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.challenge.entity.enums.ActionButtonStatus;
import com.hrr.backend.domain.challenge.repository.ChallengeDayJoinRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeLikeRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeWaitRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.s3.S3UrlUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceInfoTest {

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private VerificationRepository verificationRepository;
    @Mock private ChallengeLikeRepository challengeLikeRepository;
    @Mock private ChallengeWaitRepository challengeWaitRepository;
    @Mock private ChallengeConverter challengeConverter;
    @Mock private ChallengeDayJoinRepository challengeDayJoinRepository;
    @Mock private S3UrlUtil s3UrlUtil;

    /**
     * 1. 참여자(Participant) 관련 시나리오
     */
    @Test
    @DisplayName("상황 1: 참여자 + 라운드 시작 전 -> UPCOMING (라운드 시작 전)")
    void participant_beforeStart_returns_UPCOMING() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        setRoundDate(challenge, LocalDate.now().plusDays(1));
        setChallengeStatus(challenge, ChallengeStatus.UPCOMING, 10, 30);
        setVerificationTime(challenge, LocalTime.of(9, 0), LocalTime.of(18, 0));

        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.JOINED);
        mockConverter(ActionButtonStatus.UPCOMING);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.UPCOMING);
    }

    @Test
    @DisplayName("상황 2: 참여자 + 오늘이 인증 요일이 아님 -> NOT_DAY (인증 요일 아님)")
    void participant_notVerificationDay_returns_NOT_DAY() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        ChallengeDayJoin otherDayJoin = mock(ChallengeDayJoin.class);
        given(otherDayJoin.getDayOfWeek()).willReturn(getOtherDayEnum());
        given(challenge.getChallengeDays()).willReturn(List.of(otherDayJoin));

        setRoundDate(challenge, LocalDate.now().minusDays(1));
        setVerificationTime(challenge, LocalTime.of(9, 0), LocalTime.of(18, 0));
        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 10, 30);

        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.JOINED);
        mockConverter(ActionButtonStatus.NOT_DAY);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.NOT_DAY);
    }

    @Test
    @DisplayName("상황 3: 참여자 + 인증 요일 맞음 + 인증 시간대 아님 -> NOT_TIME (인증 시간 아님)")
    void participant_notVerificationTime_returns_NOT_TIME() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        setRoundDate(challenge, LocalDate.now().minusDays(1));
        setVerificationTime(challenge, LocalTime.of(0, 0), LocalTime.of(0, 1));

        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.JOINED);
        mockConverter(ActionButtonStatus.NOT_TIME);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.NOT_TIME);
    }

    @Test
    @DisplayName("상황 4: 참여자 + 모든 조건 충족 + 미인증 -> AVAILABLE (인증하기)")
    void participant_readyToVerify_returns_AVAILABLE() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        setRoundDate(challenge, LocalDate.now().minusDays(1));
        setVerificationTime(challenge, LocalTime.MIN, LocalTime.MAX);

        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.JOINED);
        given(verificationRepository.existsTodayVerification(any(), any(), any(), any())).willReturn(false);
        mockConverter(ActionButtonStatus.AVAILABLE);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.AVAILABLE);
    }

    @Test
    @DisplayName("상황 5: 참여자 + 모든 조건 충족 + 이미 인증 완료 -> DONE (인증 완료)")
    void participant_alreadyVerified_returns_DONE() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        setRoundDate(challenge, LocalDate.now().minusDays(1));
        setVerificationTime(challenge, LocalTime.MIN, LocalTime.MAX);

        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.JOINED);
        given(verificationRepository.existsTodayVerification(any(), any(), any(), any())).willReturn(true);
        mockConverter(ActionButtonStatus.DONE);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.DONE);
    }

    /**
     * 2. 미참여자(Guest) 관련 시나리오
     */
    @Test
    @DisplayName("상황 6: 미참여자 + 모집중 + 자리 있음 -> JOIN (참가하기)")
    void guest_recruiting_hasSpace_returns_JOIN() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        setChallengeStatus(challenge, ChallengeStatus.RECRUITING, 5, 10);
        setRoundDate(challenge, LocalDate.now());

        mockFetchingChallenge(challengeId, challenge);
        mockGuest(user, challenge);
        mockConverter(ActionButtonStatus.JOIN);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.JOIN);
    }

    @Test
    @DisplayName("상황 7: 미참여자 + 모집중 + 만석 -> WAITLIST (빈자리 알림)")
    void guest_recruiting_full_returns_WAITLIST() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        setChallengeStatus(challenge, ChallengeStatus.RECRUITING, 30, 30);
        setRoundDate(challenge, LocalDate.now());

        mockFetchingChallenge(challengeId, challenge);
        mockGuest(user, challenge);
        given(challengeWaitRepository.existsByUserAndChallenge(user, challenge)).willReturn(false);
        mockConverter(ActionButtonStatus.WAITLIST);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.WAITLIST);
    }

    @Test
    @DisplayName("상황 7-1: 미참여자 + 모집중 + 만석 + 빈자리 알림 신청 완료 -> WAITLISTED")
    void guest_recruiting_full_waitRegistered_returns_WAITLISTED() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        setChallengeStatus(challenge, ChallengeStatus.RECRUITING, 30, 30);
        setRoundDate(challenge, LocalDate.now());

        mockFetchingChallenge(challengeId, challenge);
        mockGuest(user, challenge);
        given(challengeWaitRepository.existsByUserAndChallenge(user, challenge)).willReturn(true);
        mockConverter(ActionButtonStatus.WAITLISTED);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.WAITLISTED);
    }

    @Test
    @DisplayName("상황 8: 챌린지 종료 -> FINISHED (종료된 챌린지)")
    void challenge_finished_returns_FINISHED() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        setChallengeStatus(challenge, ChallengeStatus.FINISHED, 10, 30);
        setRoundDate(challenge, LocalDate.now().minusDays(30));
        setVerificationTime(challenge, LocalTime.now().minusHours(1), LocalTime.now().plusHours(1));

        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.JOINED);
        mockConverter(ActionButtonStatus.FINISHED);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.FINISHED);
    }

    @Test
    @DisplayName("상황 9: 미참여자 + 진행중 + 자리 있음 -> JOIN (중도 참여)")
    void guest_ongoing_hasSpace_returns_JOIN() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 5, 10);
        setRoundDate(challenge, LocalDate.now().minusDays(5));

        mockFetchingChallenge(challengeId, challenge);
        mockGuest(user, challenge);
        mockConverter(ActionButtonStatus.JOIN);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.JOIN);
    }

    /**
     * 3. 방장 및 예외 시나리오
     */
    @Test
    @DisplayName("상황 10: 방장이 탈퇴함 -> 정상 조회 성공")
    void owner_inactive_returns_info_successfully() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();
        setRoundDate(challenge, LocalDate.now());
        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 10, 30);

        User owner = mock(User.class);
        given(owner.getUserStatus()).willReturn(UserStatus.INACTIVE);
        UserChallenge ownerUc = mock(UserChallenge.class);
        given(ownerUc.getUser()).willReturn(owner);

        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByChallengeIdAndRole(challengeId, UserChallengeRole.OWNER))
                .willReturn(Optional.of(ownerUc));

        mockGuest(user, challenge);
        mockConverter(ActionButtonStatus.JOIN);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result).isNotNull();
    }

    /**
     * 4. 종료된 챌린지 관련 시나리오 (유저 상태별 일관성 검증)
     */
    @Test
    @DisplayName("상황 11: 종료된 챌린지 + 미참여자(기록 없음) -> FINISHED")
    void finished_guest_returns_FINISHED() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        setChallengeStatus(challenge, ChallengeStatus.FINISHED, 5, 10);
        mockFetchingChallenge(challengeId, challenge);
        mockGuest(user, challenge);
        mockConverter(ActionButtonStatus.FINISHED);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.FINISHED);
    }

    @Test
    @DisplayName("상황 12: 종료된 챌린지 + 참여 완료 유저(JOINED) -> FINISHED")
    void finished_joinedUser_returns_FINISHED() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        setChallengeStatus(challenge, ChallengeStatus.FINISHED, 10, 10);
        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.JOINED);
        mockConverter(ActionButtonStatus.FINISHED);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.FINISHED);
    }

    @Test
    @DisplayName("상황 13: 종료된 챌린지 + 하차한 유저(DROPPED) -> FINISHED")
    void finished_droppedUser_returns_FINISHED() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        setChallengeStatus(challenge, ChallengeStatus.FINISHED, 5, 10);
        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.DROPPED);
        mockConverter(ActionButtonStatus.FINISHED);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.FINISHED);
    }

    @Test
    @DisplayName("상황 14: 종료된 챌린지 + 강퇴된 유저(KICKED) -> FINISHED")
    void finished_kickedUser_returns_FINISHED() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        setChallengeStatus(challenge, ChallengeStatus.FINISHED, 5, 10);
        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.KICKED); // 퇴출됨
        mockConverter(ActionButtonStatus.FINISHED);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.FINISHED);
    }

    /**
     * 5. 진행 중인 챌린지 + 특수 상태 시나리오
     */
    @Test
    @DisplayName("상황 15: 진행 중 + 하차(DROPPED)한 유저는 재참여 가능하므로 JOIN")
    void ongoing_droppedUser_returns_JOIN() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 5, 10);
        setRoundDate(challenge, LocalDate.now());

        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.DROPPED);
        mockConverter(ActionButtonStatus.JOIN);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.JOIN);
    }

    @Test
    @DisplayName("상황 16: 진행 중 + 기존 퇴출(KICKED) 기록이 있는 유저는 일반 미참여자로 처리되어 JOIN")
    void ongoing_kickedUser_returns_JOIN() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 5, 10);
        setRoundDate(challenge, LocalDate.now());

        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.KICKED);
        mockConverter(ActionButtonStatus.JOIN);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.JOIN);
    }

    @Test
    @DisplayName("상황 17: 진행 중 + 기존 퇴출(KICKED) 기록 + 만석 + 빈자리 알림 미신청 -> WAITLIST")
    void ongoing_kickedUser_full_notWaitRegistered_returns_WAITLIST() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 30, 30);
        setRoundDate(challenge, LocalDate.now());

        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.KICKED);
        given(challengeWaitRepository.existsByUserAndChallenge(user, challenge)).willReturn(false);
        mockConverter(ActionButtonStatus.WAITLIST);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.WAITLIST);
    }

    @Test
    @DisplayName("상황 18: 진행 중 + 기존 퇴출(KICKED) 기록 + 만석 + 빈자리 알림 신청 완료 -> WAITLISTED")
    void ongoing_kickedUser_full_waitRegistered_returns_WAITLISTED() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 30, 30);
        setRoundDate(challenge, LocalDate.now());

        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.KICKED);
        given(challengeWaitRepository.existsByUserAndChallenge(user, challenge)).willReturn(true);
        mockConverter(ActionButtonStatus.WAITLISTED);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.WAITLISTED);
    }

    @Test
    @DisplayName("상황 19: 진행 중 + 기존 퇴출(KICKED) 기록 + 참여 개수 제한 -> MAX_LIMIT_EXCEEDED")
    void ongoing_kickedUser_maxJoined_returns_MAX_LIMIT_EXCEEDED() {
        Long challengeId = 1L;
        Long userId = 10L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        given(user.getId()).willReturn(userId);
        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 5, 10);
        setRoundDate(challenge, LocalDate.now());

        mockFetchingChallenge(challengeId, challenge);
        mockParticipantWithStatus(user, challenge, ChallengeJoinStatus.KICKED);
        given(challengeRepository.countByUserIdAndStatus(userId, ChallengeJoinStatus.JOINED)).willReturn(5L);
        mockConverter(ActionButtonStatus.MAX_LIMIT_EXCEEDED);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.MAX_LIMIT_EXCEEDED);
    }
    /**
     * Helper Methods
     */
    private Challenge createChallengeBase() {
        Challenge challenge = mock(Challenge.class);
        ChallengeDayJoin todayJoin = mock(ChallengeDayJoin.class);
        lenient().when(todayJoin.getDayOfWeek()).thenReturn(getTodayChallengeDayEnum());
        lenient().when(challenge.getChallengeDays()).thenReturn(List.of(todayJoin));
        return challenge;
    }

    private void mockFetchingChallenge(Long id, Challenge challenge) {
        given(challengeRepository.findByIdWithDays(id)).willReturn(Optional.of(challenge));
        lenient().when(userChallengeRepository.findByChallengeIdAndRole(id, UserChallengeRole.OWNER))
                .thenReturn(Optional.of(mock(UserChallenge.class)));
    }

    private void mockGuest(User user, Challenge challenge) {
        given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.empty());
    }

    private void mockParticipantWithStatus(User user, Challenge challenge, ChallengeJoinStatus status) {
        UserChallenge uc = mock(UserChallenge.class);
        lenient().when(uc.getId()).thenReturn(100L);
        lenient().when(uc.getStatus()).thenReturn(status);
        given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.of(uc));
    }

    private void setRoundDate(Challenge challenge, LocalDate startDate) {
        Round round = mock(Round.class);
        given(round.getStartDate()).willReturn(startDate);
        given(round.getEndDate()).willReturn(startDate.plusDays(20));
        given(challenge.getCurrentRound()).willReturn(round);
    }

    private void setVerificationTime(Challenge challenge, LocalTime start, LocalTime end) {
        lenient().when(challenge.getVerifyStartTime()).thenReturn(start);
        lenient().when(challenge.getVerifyEndTime()).thenReturn(end);
    }

    private void setChallengeStatus(Challenge challenge, ChallengeStatus status, int current, int max) {
        lenient().when(challenge.getStatus()).thenReturn(status);
        lenient().when(challenge.getCurrentParticipants()).thenReturn(current);
        lenient().when(challenge.getMaxParticipants()).thenReturn(max);
    }

    private ChallengeDays getTodayChallengeDayEnum() {
        return ChallengeDays.valueOf(LocalDate.now().getDayOfWeek().name());
    }

    private ChallengeDays getOtherDayEnum() {
        ChallengeDays today = getTodayChallengeDayEnum();
        return today == ChallengeDays.MONDAY ? ChallengeDays.TUESDAY : ChallengeDays.MONDAY;
    }

    private void mockConverter(ActionButtonStatus expectedStatus) {
        given(challengeConverter.toHeaderInfoDto(any(), any(), anyBoolean(), any(), any(), anyLong(), anyBoolean(), anyBoolean(), any(), anyBoolean()))
                .willAnswer(invocation -> ChallengeResponseDto.HeaderInfoDto.builder()
                        .actionButtonStatus(invocation.getArgument(8))
                        .build());
    }
}
