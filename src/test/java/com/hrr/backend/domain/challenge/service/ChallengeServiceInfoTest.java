package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.converter.ChallengeConverter;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.challenge.entity.enums.ActionButtonStatus;
import com.hrr.backend.domain.challenge.repository.ChallengeLikeRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.UserChallengeRole;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.common.enums.ChallengeDays;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceInfoTest {

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private VerificationRepository verificationRepository;
    @Mock private ChallengeLikeRepository challengeLikeRepository;
    @Mock private ChallengeConverter challengeConverter;

    // 나머지 Repository들은 이 테스트 범위(상단 정보 조회)에서 쓰이지 않아 생략 가능

    /**
     * 1. 참여자(Participant) 관련 시나리오
     */
    @Test
    @DisplayName("상황 1: 참여자 + UPCOMING 상태(라운드 시작 전) -> CERTIFIED (D-Day)")
    void participant_upcoming_beforeStart_returns_CERTIFIED() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        // 조건: 챌린지 시작일이 '내일'임
        setRoundDate(challenge, LocalDate.now().plusDays(1));
        // 조건: 상태는 UPCOMING
        setChallengeStatus(challenge, ChallengeStatus.UPCOMING, 10, 30);
        // 조건: 인증 시간대 내 (시간은 맞지만 날짜가 안 맞음)
        setVerificationTime(challenge, LocalTime.of(9, 0), LocalTime.of(18, 0));

        // Mocking
        mockFetchingChallenge(challengeId, challenge);
        mockParticipant(user, challenge, true);
        mockConverter(ActionButtonStatus.CERTIFIED);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.CERTIFIED);
    }

    @Test
    @DisplayName("상황 2: 참여자 + 오늘이 인증 요일이 아님 -> CERTIFIED (D-Day)")
    void participant_notVerificationDay_returns_CERTIFIED() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        // 조건: 오늘이 아닌 다른 요일만 설정됨
        ChallengeDayJoin otherDayJoin = mock(ChallengeDayJoin.class);
        given(otherDayJoin.getDayOfWeek()).willReturn(getOtherDayEnum());
        given(challenge.getChallengeDays()).willReturn(List.of(otherDayJoin));

        // 조건: 라운드 시작됨, 시간 맞음
        setRoundDate(challenge, LocalDate.now().minusDays(1));
        setVerificationTime(challenge, LocalTime.of(9, 0), LocalTime.of(18, 0));
        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 10, 30);

        // Mocking
        mockFetchingChallenge(challengeId, challenge);
        mockParticipant(user, challenge, true);
        mockConverter(ActionButtonStatus.CERTIFIED);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.CERTIFIED);
    }

    @Test
    @DisplayName("상황 3: 참여자 + 인증 요일 맞음 + 인증 시간대 아님 -> CERTIFIED (D-Day)")
    void participant_notVerificationTime_returns_CERTIFIED() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase(); // 오늘 요일 포함

        setRoundDate(challenge, LocalDate.now().minusDays(1));

        // 현재 시간과 겹치지 않도록 이른 시간대 고정
        setVerificationTime(challenge, LocalTime.of(0, 0), LocalTime.of(0, 1));

        // Mocking
        mockFetchingChallenge(challengeId, challenge);
        mockParticipant(user, challenge, true);
        mockConverter(ActionButtonStatus.CERTIFIED);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.CERTIFIED);
    }

    @Test
    @DisplayName("상황 4: 참여자 + 모든 조건 충족 + 아직 인증 안 함 -> CERTIFY_AVAILABLE (인증하기)")
    void participant_readyToVerify_returns_AVAILABLE() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        setRoundDate(challenge, LocalDate.now().minusDays(1));
        // 조건: 현재 시간이 인증 시간 내에 포함됨
        setVerificationTime(challenge, LocalTime.MIN, LocalTime.MAX);

        // Mocking
        mockFetchingChallenge(challengeId, challenge);
        mockParticipant(user, challenge, true);

        // 조건: 오늘 완료된 인증 없음 (false)
        given(verificationRepository.existsTodayVerification(any(), any(), any(), any())).willReturn(false);

        mockConverter(ActionButtonStatus.CERTIFY_AVAILABLE);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.CERTIFY_AVAILABLE);
    }

    @Test
    @DisplayName("상황 5: 참여자 + 모든 조건 충족 + 이미 인증 완료함 -> CERTIFIED (완료)")
    void participant_alreadyVerified_returns_CERTIFIED() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();

        setRoundDate(challenge, LocalDate.now().minusDays(1));
        setVerificationTime(challenge, LocalTime.MIN, LocalTime.MAX);

        // Mocking
        mockFetchingChallenge(challengeId, challenge);
        mockParticipant(user, challenge, true);

        // 조건: 오늘 완료된 인증 있음 (true)
        given(verificationRepository.existsTodayVerification(any(), any(), any(), any())).willReturn(true);

        mockConverter(ActionButtonStatus.CERTIFIED);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.CERTIFIED);
    }

    /**
     * 2. 미참여자(Guest) 관련 시나리오
     */
    @Test
    @DisplayName("상황 6: 미참여자 + 모집중 + 자리 있음 -> JOIN (참가하기)")
    void guest_recruiting_hasSpace_returns_JOIN() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        // 조건: 모집중, 인원 5/10
        setChallengeStatus(challenge, ChallengeStatus.RECRUITING, 5, 10);
        setRoundDate(challenge, LocalDate.now());

        // Mocking
        mockFetchingChallenge(challengeId, challenge);
        mockParticipant(user, challenge, false); // 미참여자

        mockConverter(ActionButtonStatus.JOIN);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.JOIN);
    }

    @Test
    @DisplayName("상황 7: 미참여자 + 모집중 + 만석 -> WAITLIST (빈자리 알림)")
    void guest_recruiting_full_returns_WAITLIST() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        // 조건: 모집중이지만 인원 30/30 (만석)
        setChallengeStatus(challenge, ChallengeStatus.RECRUITING, 30, 30);
        setRoundDate(challenge, LocalDate.now());

        // Mocking
        mockFetchingChallenge(challengeId, challenge);
        mockParticipant(user, challenge, false);

        mockConverter(ActionButtonStatus.WAITLIST);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.WAITLIST);
    }

    /**
     * 3. 공통/종료 시나리오
     */

    @Test
    @DisplayName("상황 8: 챌린지 종료(FINISHED) -> 무조건 DISABLED")
    void challenge_finished_returns_DISABLED() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        // 조건: 종료됨
        setChallengeStatus(challenge, ChallengeStatus.FINISHED, 10, 30);
        setRoundDate(challenge, LocalDate.now().minusDays(30));

        // NPE 방지를 위한 시간 설정
        setVerificationTime(challenge, LocalTime.now().minusHours(1), LocalTime.now().plusHours(1));

        // Mocking
        mockFetchingChallenge(challengeId, challenge);
        mockParticipant(user, challenge, true); // 참여자여도 비활성화되어야 함

        mockConverter(ActionButtonStatus.DISABLED);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.DISABLED);
    }

    @Test
    @DisplayName("상황 9: 미참여자 + 진행중(ONGOING) + 자리 있음 -> JOIN (중도 참여 허용 확인)")
    void guest_ongoing_hasSpace_returns_JOIN() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = mock(Challenge.class);

        // 조건: 진행 중(ONGOING), 인원 여유 있음
        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 5, 10);
        setRoundDate(challenge, LocalDate.now().minusDays(5)); // 이미 시작된지 5일 지남

        // Mocking
        mockFetchingChallenge(challengeId, challenge);
        mockParticipant(user, challenge, false);

        mockConverter(ActionButtonStatus.JOIN);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.JOIN);
    }

    /**
     * 4. 방장 상태 관련 시나리오 (신규 추가)
     */
    @Test
    @DisplayName("상황 10: 방장이 탈퇴함 (UserStatus != ACTIVE) -> 정상 조회")
    void owner_inactive_returns_info_successfully() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();
        setRoundDate(challenge, LocalDate.now());
        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 10, 30);

        // 방장이 존재하지만 탈퇴 상태
        User owner = mock(User.class);
        given(owner.getUserStatus()).willReturn(UserStatus.INACTIVE);
        UserChallenge ownerUc = mock(UserChallenge.class);
        given(ownerUc.getUser()).willReturn(owner);

        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByChallengeIdAndRole(challengeId, UserChallengeRole.OWNER))
                .willReturn(Optional.of(ownerUc));

        mockParticipant(user, challenge, false);
        mockConverter(ActionButtonStatus.JOIN);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("상황 11: 방장 데이터가 아예 없음 (owner == null) -> 에러 없이 조회")
    void owner_not_found_returns_info_successfully() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();
        setRoundDate(challenge, LocalDate.now());
        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 10, 30);

        // 방장 정보가 Optional.empty()
        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByChallengeIdAndRole(challengeId, UserChallengeRole.OWNER))
                .willReturn(Optional.empty());

        mockParticipant(user, challenge, false);
        mockConverter(ActionButtonStatus.JOIN);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result).isNotNull();
    }

    /**
     * 5. 참여 제한 관련 시나리오
     */
    @Test
    @DisplayName("상황 12: 미참여자 + 현재 참여 중인 챌린지가 5개임 -> DISABLED (참가 불가)")
    void guest_already_joined_five_challenges_returns_DISABLED() {
        // Given
        Long challengeId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);

        Challenge challenge = mock(Challenge.class);
        // 조건: 모집중이며 자리가 남아있음
        setChallengeStatus(challenge, ChallengeStatus.RECRUITING, 5, 10);
        setRoundDate(challenge, LocalDate.now());

        // Mocking
        mockFetchingChallenge(challengeId, challenge);
        mockParticipant(user, challenge, false); // 이 챌린지에는 미참여 상태

        // 핵심 조건: 이미 참여 중인 챌린지 개수가 5개임
        given(challengeRepository.countByUserIdAndStatus(eq(user.getId()), any()))
                .willReturn(5L);

        // ActionButtonStatus.DISABLED가 반환될 것을 기대
        mockConverter(ActionButtonStatus.DISABLED);

        // When
        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);

        // Then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.DISABLED);
    }

    /**
     * Helper Methods (테스트 설정을 쉽게 하기 위한 도구들)
     */

    /**
     * 오늘 요일이 포함된 Mock Challenge 생성
     */
    private Challenge createChallengeBase() {
        Challenge challenge = mock(Challenge.class);
        ChallengeDayJoin todayJoin = mock(ChallengeDayJoin.class);

        // lenient 사용
        lenient().when(todayJoin.getDayOfWeek()).thenReturn(getTodayChallengeDayEnum());
        lenient().when(challenge.getChallengeDays()).thenReturn(List.of(todayJoin));

        return challenge;
    }

    private void mockFetchingChallenge(Long id, Challenge challenge) {
        given(challengeRepository.findByIdWithDays(id)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByChallengeIdAndRole(id, UserChallengeRole.OWNER))
                .willReturn(Optional.of(mock(UserChallenge.class))); // 방장 정보는 에러만 안 나게 처리
    }

    private void mockParticipant(User user, Challenge challenge, boolean isParticipant) {
        if (isParticipant) {
            UserChallenge uc = mock(UserChallenge.class);
            given(uc.getId()).willReturn(100L);
            given(userChallengeRepository.findByUserAndChallenge(user, challenge))
                    .willReturn(Optional.of(uc));
        } else {
            given(userChallengeRepository.findByUserAndChallenge(user, challenge))
                    .willReturn(Optional.empty());
        }
    }

    private void setRoundDate(Challenge challenge, LocalDate startDate) {
        Round round = mock(Round.class);
        given(round.getStartDate()).willReturn(startDate);
        // endDate는 D-Day 계산용이지만 로직 흐름에는 큰 영향 없어서 임의 설정
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
    // 오늘 요일 구하기
    private ChallengeDays getTodayChallengeDayEnum() {
        return ChallengeDays.valueOf(LocalDate.now().getDayOfWeek().name());
    }

    // 오늘이 아닌 다른 요일 구하기
    private ChallengeDays getOtherDayEnum() {
        ChallengeDays today = getTodayChallengeDayEnum();
        return today == ChallengeDays.MONDAY ? ChallengeDays.TUESDAY : ChallengeDays.MONDAY;
    }

    // Converter가 Status를 그대로 통과시키도록 Stubbing
    private void mockConverter(ActionButtonStatus expectedStatus) {
        given(challengeConverter.toHeaderInfoDto(any(), any(), anyBoolean(), any(), any(), anyLong(), anyBoolean(), anyBoolean(), any()))
                .willAnswer(invocation -> ChallengeResponseDto.HeaderInfoDto.builder()
                        .actionButtonStatus(invocation.getArgument(8))
                        .build());
    }
}