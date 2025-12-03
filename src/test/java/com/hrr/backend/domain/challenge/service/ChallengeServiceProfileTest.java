package com.hrr.backend.domain.challenge.service;

import com.hrr.backend.domain.challenge.converter.ChallengeConverter;
import com.hrr.backend.domain.challenge.dto.ChallengeResponseDto;
import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.entity.ChallengeDayJoin;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.common.enums.ChallengeDays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceProfileTest {

    @InjectMocks
    private ChallengeServiceImpl challengeService;

    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private VerificationRepository verificationRepository;

    // Converter는 로직이 있으므로 실제 객체(Spy) 사용
    @Spy private ChallengeConverter challengeConverter;

    @Test
    @DisplayName("1. [부분 인증] 인증 요일이 월/목인데 '월요일'만 인증한 경우 -> 리스트에 MONDAY 하나만 존재")
    void getChallengeProfile_PartialVerification() {
        // Given
        Long challengeId = 1L;
        User user = User.builder().id(100L).build();
        // 챌린지: 월, 목 인증 목표
        Challenge challenge = createChallenge(challengeId, "부분 인증 테스트", List.of(ChallengeDays.MONDAY, ChallengeDays.THURSDAY));

        // 유저 참여 중
        mockParticipating(user, challenge);

        // 인증 내역: 이번 주 월요일 1건
        List<Verification> verifications = List.of(
                createVerification(DayOfWeek.MONDAY)
        );

        // Mocking
        mockRepositoryCalls(user, challenge, verifications);

        // When
        ChallengeResponseDto.ChallengeProfileDto result = challengeService.getChallengeProfile(user, challengeId);

        // Then
        assertThat(result.getIsParticipating()).isTrue();
        assertThat(result.getTargetDays()).contains(ChallengeDays.MONDAY, ChallengeDays.THURSDAY); // 목표는 2개
        assertThat(result.getVerifiedDaysThisWeek()).hasSize(1); // 인증은 1개
        assertThat(result.getVerifiedDaysThisWeek()).containsOnly(ChallengeDays.MONDAY); // 월요일만 포함
    }

    @Test
    @DisplayName("2. [완료 인증] 인증 요일이 월/목이고 둘 다 인증한 경우 -> 리스트에 둘 다 존재")
    void getChallengeProfile_FullVerification() {
        // Given
        Long challengeId = 1L;
        User user = User.builder().id(100L).build();
        Challenge challenge = createChallenge(challengeId, "완료 인증 테스트", List.of(ChallengeDays.MONDAY, ChallengeDays.THURSDAY));

        mockParticipating(user, challenge);

        // 인증 내역: 월요일, 목요일 2건
        List<Verification> verifications = List.of(
                createVerification(DayOfWeek.MONDAY),
                createVerification(DayOfWeek.THURSDAY)
        );

        mockRepositoryCalls(user, challenge, verifications);

        // When
        ChallengeResponseDto.ChallengeProfileDto result = challengeService.getChallengeProfile(user, challengeId);

        // Then
        assertThat(result.getVerifiedDaysThisWeek()).hasSize(2);
        assertThat(result.getVerifiedDaysThisWeek()).containsExactlyInAnyOrder(ChallengeDays.MONDAY, ChallengeDays.THURSDAY);
    }

    @Test
    @DisplayName("3. [주 시작일 인증] 일요일(주의 시작)에 인증한 경우 -> 이번 주 현황에 포함되어야 한다")
    void getChallengeProfile_SundayVerification() {
        // Given
        Long challengeId = 1L;
        User user = User.builder().id(100L).build();
        Challenge challenge = createChallenge(challengeId, "일요일 인증 테스트", List.of(ChallengeDays.SUNDAY));

        mockParticipating(user, challenge);

        // 인증 내역: 일요일 (Service 로직상 주의 시작일)
        List<Verification> verifications = List.of(
                createVerification(DayOfWeek.SUNDAY)
        );

        mockRepositoryCalls(user, challenge, verifications);

        // When
        ChallengeResponseDto.ChallengeProfileDto result = challengeService.getChallengeProfile(user, challengeId);

        // Then
        assertThat(result.getVerifiedDaysThisWeek()).contains(ChallengeDays.SUNDAY);
    }

    @Test
    @DisplayName("4. [미참여] 챌린지에 참여하지 않은 경우 -> verifiedDaysThisWeek는 null이어야 한다")
    void getChallengeProfile_NotParticipating() {
        // Given
        Long challengeId = 1L;
        User user = User.builder().id(100L).build();
        Challenge challenge = createChallenge(challengeId, "미참여 테스트", List.of(ChallengeDays.MONDAY));

        // Mocking (참여 정보 없음)
        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));
        given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.empty());

        // When
        ChallengeResponseDto.ChallengeProfileDto result = challengeService.getChallengeProfile(user, challengeId);

        // Then
        assertThat(result.getIsParticipating()).isFalse();
        assertThat(result.getVerifiedDaysThisWeek()).isNull();
    }

    @Test
    @DisplayName("5. [기간 제외] 지난 주(예: 지난 목요일)에 인증했더라도, 이번 주(일~토) 범위 밖이므로 안 떠야 한다")
    void getChallengeProfile_LastWeekVerification_ShouldNotAppear() {
        // Given
        Long challengeId = 1L;
        User user = User.builder().id(100L).build();
        Challenge challenge = createChallenge(challengeId, "지난 주 인증 제외 테스트", List.of(ChallengeDays.THURSDAY));

        // 1. 참여 정보 Mocking
        mockParticipating(user, challenge);

        // 2. 챌린지 정보 조회 Mocking
        given(challengeRepository.findByIdWithDays(challengeId)).willReturn(Optional.of(challenge));

        // 3. 인증 내역 Mocking (지난 주 데이터라 DB 쿼리 결과에는 안 잡힘을 가정 -> 빈 리스트 반환)
        given(verificationRepository.findWeeklyVerifications(any(), any(), any(), any(), any()))
                .willReturn(Collections.emptyList());

        // When
        ChallengeResponseDto.ChallengeProfileDto result = challengeService.getChallengeProfile(user, challengeId);

        // Then
        // 1. 결과가 비어있어야 함
        assertThat(result.getVerifiedDaysThisWeek()).isEmpty();

        // 2. 서비스가 날짜 범위를 제대로 계산해서 DB에 넘겼는지 검증
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(verificationRepository).findWeeklyVerifications(
                eq(user.getId()),
                eq(challengeId),
                captor.capture(),
                captor.capture(),
                eq(VerificationStatus.COMPLETED)
        );

        LocalDateTime capturedStart = captor.getAllValues().get(0);
        LocalDateTime capturedEnd = captor.getAllValues().get(1);

        // 시작일이 일요일, 종료일이 토요일이어야 함
        assertThat(capturedStart.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(capturedEnd.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
    }

    // 챌린지 생성 헬퍼
    private Challenge createChallenge(Long id, String title, List<ChallengeDays> targetDays) {
        Challenge challenge = Challenge.builder()
                .id(id)
                .title(title)
                .rule("규칙")
                .verifyStartTime(LocalTime.of(9, 0))
                .verifyEndTime(LocalTime.of(18, 0))
                .build();

        // 목표 요일 추가
        for (ChallengeDays day : targetDays) {
            challenge.getChallengeDays().add(
                    ChallengeDayJoin.builder().dayOfWeek(day).challenge(challenge).build()
            );
        }
        return challenge;
    }

    // 인증 내역 생성 헬퍼 (Reflection으로 createdAt 주입)
    private Verification createVerification(DayOfWeek dayOfWeek) {
        Verification verification = Verification.builder().status(VerificationStatus.COMPLETED).build();

        // 이번 주 해당 요일의 날짜 계산
        LocalDateTime date = LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)) // 일요일로 이동
                .with(TemporalAdjusters.nextOrSame(dayOfWeek)); // 해당 요일로 이동

        if (dayOfWeek == DayOfWeek.SUNDAY) {
            date = LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        }

        ReflectionTestUtils.setField(verification, "createdAt", date);
        return verification;
    }

    // 참여 상태 Mocking
    private void mockParticipating(User user, Challenge challenge) {
        UserChallenge uc = UserChallenge.builder()
                .id(10L).user(user).challenge(challenge)
                .status(ChallengeJoinStatus.JOINED)
                .build();
        given(userChallengeRepository.findByUserAndChallenge(user, challenge)).willReturn(Optional.of(uc));
    }

    // Repository 호출 Mocking (Common)
    private void mockRepositoryCalls(User user, Challenge challenge, List<Verification> verifications) {
        given(challengeRepository.findByIdWithDays(challenge.getId())).willReturn(Optional.of(challenge));
        // any()를 사용한 날짜 범위 계산 로직
        given(verificationRepository.findWeeklyVerifications(
                eq(user.getId()),
                eq(challenge.getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(VerificationStatus.COMPLETED)
        )).willReturn(verifications);
    }
}