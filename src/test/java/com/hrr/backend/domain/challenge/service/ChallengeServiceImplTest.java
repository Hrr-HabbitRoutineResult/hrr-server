package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.converter.ChallengeConverter;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.challenge.entity.enums.ActionButtonStatus;
import com.hrr.backend.domain.challenge.repository.ChallengeLikeRepository;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
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
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceImplTest {

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private VerificationRepository verificationRepository;
    @Mock private ChallengeLikeRepository challengeLikeRepository;
    @Mock private ChallengeConverter challengeConverter;

    // 나머지 생성자 파라미터들은 이 테스트에서 안 쓰여서 생략

    @Test
    @DisplayName("참여자 + 오늘이 인증 요일이 아님 -> CERTIFIED (D-DAY)")
    void participant_notVerificationDay_returns_CERTIFIED() {
        // given
        Long challengeId = 1L;
        User user = mock(User.class);

        Challenge challenge = mock(Challenge.class);
        // 오늘이 아닌 다른 요일만 포함되도록 설정
        ChallengeDayJoin otherDayJoin = mock(ChallengeDayJoin.class);
        given(otherDayJoin.getDayOfWeek()).willReturn(getNotTodayChallengeDay());
        given(challenge.getChallengeDays()).willReturn(List.of(otherDayJoin));

        // 인증 시간대 (checkTodayVerification에서 사용되므로 세팅 필요)
        LocalTime now = LocalTime.now();
        setVerificationTime(challenge, now.minusHours(1), now.plusHours(1));

        // 챌린지 조회 (findByIdWithDays)
        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));

        // 참여자
        UserChallenge userChallenge = mock(UserChallenge.class);
        given(userChallenge.getId()).willReturn(100L);
        given(userChallengeRepository.findByUserAndChallenge(user, challenge))
                .willReturn(Optional.of(userChallenge));

        // 오늘 인증 여부는 어떤 값이어도 상관 없음 (어차피 요일에서 걸림)
        given(verificationRepository.existsTodayVerification(anyLong(), any(), any(), any()))
                .willReturn(false);

        // 방장
        given(userChallengeRepository.findOwnerByChallengeId(challengeId))
                .willReturn(Optional.of(mock(UserChallenge.class)));

        // Converter mock (status 그대로 DTO에 실어 보내기)
        mockConverter();

        // when
        ChallengeResponseDto.HeaderInfoDto result =
                challengeService.getChallengeHeaderInfo(challengeId, user);

        // then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.CERTIFIED);
    }

    @Test
    @DisplayName("참여자 + 인증 요일 + 인증 시간대 아님 -> CERTIFIED")
    void participant_verificationDay_notInTime_returns_CERTIFIED() {
        // given
        Long challengeId = 1L;
        User user = mock(User.class);

        Challenge challenge = mockChallengeWithTodayAsVerificationDay();

        LocalTime now = LocalTime.now();
        // 현재 시간보다 이후로 설정해서, 지금은 인증 시간대 밖
        setVerificationTime(challenge, now.plusHours(1), now.plusHours(2));

        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));

        UserChallenge userChallenge = mock(UserChallenge.class);
        given(userChallenge.getId()).willReturn(100L);
        given(userChallengeRepository.findByUserAndChallenge(user, challenge))
                .willReturn(Optional.of(userChallenge));

        given(verificationRepository.existsTodayVerification(anyLong(), any(), any(), any()))
                .willReturn(false);

        given(userChallengeRepository.findOwnerByChallengeId(challengeId))
                .willReturn(Optional.of(mock(UserChallenge.class)));

        mockConverter();

        // when
        ChallengeResponseDto.HeaderInfoDto result =
                challengeService.getChallengeHeaderInfo(challengeId, user);

        // then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.CERTIFIED);
    }

    @Test
    @DisplayName("참여자 + 인증 요일 + 인증 시간대 + 미인증 -> CERTIFY_AVAILABLE (인증하기)")
    void participant_inTime_notVerified_returns_CERTIFY_AVAILABLE() {
        // given
        Long challengeId = 1L;
        User user = mock(User.class);

        Challenge challenge = mockChallengeWithTodayAsVerificationDay();

        LocalTime now = LocalTime.now();
        // 현재 시간 포함
        setVerificationTime(challenge, now.minusHours(1), now.plusHours(1));

        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));

        UserChallenge userChallenge = mock(UserChallenge.class);
        given(userChallenge.getId()).willReturn(100L);
        given(userChallengeRepository.findByUserAndChallenge(user, challenge))
                .willReturn(Optional.of(userChallenge));

        // 오늘 인증 내역 없음
        given(verificationRepository.existsTodayVerification(anyLong(), any(), any(), any()))
                .willReturn(false);

        given(userChallengeRepository.findOwnerByChallengeId(challengeId))
                .willReturn(Optional.of(mock(UserChallenge.class)));

        mockConverter();

        // when
        ChallengeResponseDto.HeaderInfoDto result =
                challengeService.getChallengeHeaderInfo(challengeId, user);

        // then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.CERTIFY_AVAILABLE);
    }

    @Test
    @DisplayName("참여자 + 인증 요일 + 인증 시간대 + 이미 인증함 -> CERTIFIED")
    void participant_inTime_verified_returns_CERTIFIED() {
        // given
        Long challengeId = 1L;
        User user = mock(User.class);

        Challenge challenge = mockChallengeWithTodayAsVerificationDay();

        LocalTime now = LocalTime.now();
        setVerificationTime(challenge, now.minusHours(1), now.plusHours(1));

        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));

        UserChallenge userChallenge = mock(UserChallenge.class);
        given(userChallenge.getId()).willReturn(100L);
        given(userChallengeRepository.findByUserAndChallenge(user, challenge))
                .willReturn(Optional.of(userChallenge));

        // 오늘 인증 내역 있음
        given(verificationRepository.existsTodayVerification(anyLong(), any(), any(), any()))
                .willReturn(true);

        given(userChallengeRepository.findOwnerByChallengeId(challengeId))
                .willReturn(Optional.of(mock(UserChallenge.class)));

        mockConverter();

        // when
        ChallengeResponseDto.HeaderInfoDto result =
                challengeService.getChallengeHeaderInfo(challengeId, user);

        // then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.CERTIFIED);
    }

    @Test
    @DisplayName("미참여자 + 모집중 + 자리 있음 -> JOIN")
    void nonParticipant_recruiting_notFull_returns_JOIN() {
        // given
        Long challengeId = 1L;
        User user = mock(User.class);

        Challenge challenge = mock(Challenge.class);
        given(challenge.getStatus()).willReturn(ChallengeStatus.RECRUITING);
        given(challenge.getCurrentParticipants()).willReturn(5);
        given(challenge.getMaxParticipants()).willReturn(10);

        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));

        // 미참여자
        given(userChallengeRepository.findByUserAndChallenge(user, challenge))
                .willReturn(Optional.empty());

        given(userChallengeRepository.findOwnerByChallengeId(challengeId))
                .willReturn(Optional.of(mock(UserChallenge.class)));

        mockConverter();

        // when
        ChallengeResponseDto.HeaderInfoDto result =
                challengeService.getChallengeHeaderInfo(challengeId, user);

        // then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.JOIN);
    }

    @Test
    @DisplayName("미참여자 + 모집중 + 만석 -> WAITLIST")
    void nonParticipant_recruiting_full_returns_WAITLIST() {
        // given
        Long challengeId = 1L;
        User user = mock(User.class);

        Challenge challenge = mock(Challenge.class);
        given(challenge.getStatus()).willReturn(ChallengeStatus.RECRUITING);
        given(challenge.getCurrentParticipants()).willReturn(30);
        given(challenge.getMaxParticipants()).willReturn(30);

        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));

        given(userChallengeRepository.findByUserAndChallenge(user, challenge))
                .willReturn(Optional.empty());

        given(userChallengeRepository.findOwnerByChallengeId(challengeId))
                .willReturn(Optional.of(mock(UserChallenge.class)));

        mockConverter();

        // when
        ChallengeResponseDto.HeaderInfoDto result =
                challengeService.getChallengeHeaderInfo(challengeId, user);

        // then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.WAITLIST);
    }

    @Test
    @DisplayName("챌린지 FINISHED 상태 -> 항상 DISABLED")
    void finishedChallenge_returns_DISABLED() {
        // given
        Long challengeId = 1L;
        User user = mock(User.class);

        Challenge challenge = mock(Challenge.class);
        given(challenge.getStatus()).willReturn(ChallengeStatus.FINISHED);

        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));

        // 참여 여부와 관계없이 DISABLED여야 함
        given(userChallengeRepository.findByUserAndChallenge(user, challenge))
                .willReturn(Optional.empty());

        given(userChallengeRepository.findOwnerByChallengeId(challengeId))
                .willReturn(Optional.of(mock(UserChallenge.class)));

        mockConverter();

        // when
        ChallengeResponseDto.HeaderInfoDto result =
                challengeService.getChallengeHeaderInfo(challengeId, user);

        // then
        assertThat(result.getActionButtonStatus()).isEqualTo(ActionButtonStatus.DISABLED);
    }

    // =================== Helper Methods ===================

    /**
     * 오늘 요일이 인증 요일로 설정된 Challenge mock 생성
     */
    private Challenge mockChallengeWithTodayAsVerificationDay() {
        Challenge challenge = mock(Challenge.class);

        ChallengeDayJoin todayJoin = mock(ChallengeDayJoin.class);
        given(todayJoin.getDayOfWeek()).willReturn(getTodayChallengeDay());
        given(challenge.getChallengeDays()).willReturn(List.of(todayJoin));

        return challenge;
    }

    private void setVerificationTime(Challenge challenge, LocalTime start, LocalTime end) {
        given(challenge.getVerifyStartTime()).willReturn(start);
        given(challenge.getVerifyEndTime()).willReturn(end);
    }

    /**
     * Converter가 서비스에서 계산한 ActionButtonStatus를
     * 그대로 HeaderInfoDto에 실어주도록 stub
     */
    private void mockConverter() {
        given(challengeConverter.toHeaderInfoDto(
                any(Challenge.class),
                any(),            // owner user
                any(),            // startDate
                any(),            // endDate
                anyLong(),        // remainDays
                anyBoolean(),     // isParticipant
                anyBoolean(),     // isLiked
                any(ActionButtonStatus.class) // status
        )).willAnswer(invocation -> ChallengeResponseDto.HeaderInfoDto.builder()
                .actionButtonStatus(invocation.getArgument(7))
                .build());
    }

    /**
     * 오늘 요일에 해당하는 ChallengeDays enum
     */
    private ChallengeDays getTodayChallengeDay() {
        String name = LocalDate.now().getDayOfWeek().name(); // "MONDAY" ...
        return ChallengeDays.valueOf(name);
    }

    /**
     * 오늘이 아닌 다른 요일 하나 리턴
     */
    private ChallengeDays getNotTodayChallengeDay() {
        ChallengeDays today = getTodayChallengeDay();
        for (ChallengeDays day : ChallengeDays.values()) {
            if (day != today) {
                return day;
            }
        }
        // 이론상 도달 불가지만, 컴파일을 위해
        return ChallengeDays.MONDAY;
    }
}
