package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.converter.ChallengeConverter;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.challenge.entity.enums.ActionButtonStatus;
import com.hrr.backend.domain.challenge.repository.ChallengeDayJoinRepository;
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
import com.hrr.backend.global.response.SliceResponseDto;
import com.hrr.backend.global.s3.S3UrlUtil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        mockParticipant(user, challenge, true);
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
        mockParticipant(user, challenge, true);
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
        mockParticipant(user, challenge, true);
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
        mockParticipant(user, challenge, true);
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
        mockParticipant(user, challenge, true);
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
        mockParticipant(user, challenge, false);
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
        mockParticipant(user, challenge, false);
        mockConverter(ActionButtonStatus.WAITLIST);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.WAITLIST);
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
        mockParticipant(user, challenge, true);
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
        mockParticipant(user, challenge, false);
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

        mockParticipant(user, challenge, false);
        mockConverter(ActionButtonStatus.JOIN);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("상황 11: 방장 데이터 없음 -> 에러 없이 조회")
    void owner_not_found_returns_info_successfully() {
        Long challengeId = 1L;
        User user = mock(User.class);
        Challenge challenge = createChallengeBase();
        setRoundDate(challenge, LocalDate.now());
        setChallengeStatus(challenge, ChallengeStatus.ONGOING, 10, 30);

        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByChallengeIdAndRole(challengeId, UserChallengeRole.OWNER))
                .willReturn(Optional.empty());

        mockParticipant(user, challenge, false);
        mockConverter(ActionButtonStatus.JOIN);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("상황 12: 미참여자 + 참여 한도 초과 -> MAX_LIMIT_EXCEEDED")
    void guest_max_limit_exceeded_returns_MAX_LIMIT_EXCEEDED() {
        Long challengeId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(1L);

        Challenge challenge = mock(Challenge.class);
        setChallengeStatus(challenge, ChallengeStatus.RECRUITING, 5, 10);
        setRoundDate(challenge, LocalDate.now());

        mockFetchingChallenge(challengeId, challenge);
        mockParticipant(user, challenge, false);
        given(challengeRepository.countByUserIdAndStatus(eq(user.getId()), any())).willReturn(5L);

        mockConverter(ActionButtonStatus.MAX_LIMIT_EXCEEDED);

        ChallengeResponseDto.HeaderInfoDto result = challengeService.getChallengeHeaderInfo(challengeId, user);
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.MAX_LIMIT_EXCEEDED);
    }

	@Test
	@DisplayName("곧 시작하는 챌린지 필터 적용 시 날짜 계산 및 D-Day 가공 로직 검증")
	void getChallengeList_UpcomingLogic_Test() {
		LocalDateTime startTime0 = LocalDateTime.of(2026, 1, 15, 10, 0, 0);	// 오늘 시작하는 챌린지 생성용
		LocalDateTime startTime5 = LocalDateTime.of(2026, 1, 20, 23, 59, 59);	// 5일 뒤 시작하는 챌린지 생성용
		LocalDate mockToday = LocalDate.of(2026, 1, 15);	// 오늘을 26.01.15로 고정

		// 테스트용 DTO 구성 - 실제 로직에서는 Repository에서 조회해옴
		ChallengeResponseDto.InfoDto day0 = ChallengeResponseDto.InfoDto.builder()
			.challengeId(1L)
			.startDate(startTime0)
			.thumbnailUrl("test.png")
			.build();

		ChallengeResponseDto.InfoDto day5 = ChallengeResponseDto.InfoDto.builder()
			.challengeId(2L)
			.startDate(startTime5)
			.thumbnailUrl("test.png")
			.build();

		// Mock 설정 - 실제 DB를 거치지 않고 아래처럼 반환되었다고 가정
		given(challengeRepository.findChallengesWithFilters(any(), any(), any(), any(), any(), any(), any()))
			.willReturn(new SliceImpl<>(List.of(day0, day5)));
		given(challengeDayJoinRepository.findByChallengeIdIn(anyList())).willReturn(List.of());	// 요일 정보는 테스트 대상이 아니라 빈 값 반환
		lenient().when(s3UrlUtil.toFullUrl(anyString())).thenReturn("http://image.url");

		try (MockedStatic<LocalDate> mockedLocalDate =
				 mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
			// LocalDate의 static 메서드를 MockedStatic으로 전부 mock 하면 LocalDate.of() 등이 모두 null을 반환해 NPE 가능
			// 그래서 LocalDate.now()만 mock되도록 CALLS_REAL_METHODS 설정

			mockedLocalDate.when(LocalDate::now).thenReturn(mockToday);

			SliceResponseDto<ChallengeResponseDto.InfoDto> result =
				challengeService.getChallengeList(null, true, null, null, null, 0, 10);

			// 테스트 결과 검증
			assertThat(result.getContent().get(0).getIsUpcoming()).isTrue();
			assertThat(result.getContent().get(1).getIsUpcoming()).isTrue();
			assertThat(result.getContent().get(0).getDDayUntilStart()).isEqualTo(0);	// D-DAY
			assertThat(result.getContent().get(1).getDDayUntilStart()).isEqualTo(5);	// D-5
		}

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

    private void mockParticipant(User user, Challenge challenge, boolean isParticipant) {
        if (isParticipant) {
            UserChallenge uc = mock(UserChallenge.class);
            given(uc.getId()).willReturn(100L);
            given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.of(uc));
        } else {
            given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.empty());
        }
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
        given(challengeConverter.toHeaderInfoDto(any(), any(), anyBoolean(), any(), any(), anyLong(), anyBoolean(), anyBoolean(), any()))
                .willAnswer(invocation -> ChallengeResponseDto.HeaderInfoDto.builder()
                        .actionButtonStatus(invocation.getArgument(8))
                        .build());
    }
}
