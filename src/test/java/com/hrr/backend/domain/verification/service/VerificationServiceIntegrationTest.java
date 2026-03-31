package com.hrr.backend.domain.verification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.challenge.repository.ChallengeRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.round.repository.RoundRecordRepository;
import com.hrr.backend.domain.round.repository.RoundRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.user.entity.enums.ChallengeJoinStatus;
import com.hrr.backend.domain.user.entity.enums.UserStatus;
import com.hrr.backend.domain.user.repository.UserChallengeRepository;
import com.hrr.backend.domain.user.repository.UserRepository;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.repository.VerificationRepository;
import com.hrr.backend.global.common.enums.Category;
import com.hrr.backend.global.common.enums.ChallengeStatus;
import com.hrr.backend.global.common.enums.VerificationType;
import com.hrr.backend.global.exception.GlobalException;
import com.hrr.backend.global.response.ErrorCode;

@SpringBootTest
@Transactional
class VerificationServiceIntegrationTest {

    @Autowired private VerificationService verificationService;
    @Autowired private UserRepository userRepository;
    @Autowired private ChallengeRepository challengeRepository;
    @Autowired private UserChallengeRepository userChallengeRepository;
    @Autowired private RoundRepository roundRepository;
    @Autowired private RoundRecordRepository roundRecordRepository;
    @Autowired private VerificationRepository verificationRepository;

    private User author;
    private Challenge privateChallenge;
    private Verification privateVerification;

    @BeforeEach
    void setUp() {
        // 공통 Given
        // 1. 작성자 생성
        author = userRepository.save(User.builder()
                .nickname("author")
                .isPublic(true)
                .userStatus(UserStatus.ACTIVE)
                .build());

        // 2. 비공개 챌린지 생성
        privateChallenge = challengeRepository.save(Challenge.builder()
                .title("비공개 챌린지")
                .description("테스트용 챌린지")
                .isPublic(false)
                .category(Category.STUDY)
                .isViewerMode(false)
                .maxParticipants(10)
                .currentParticipants(1)
                .startDate(LocalDateTime.now())
                .verificationType(VerificationType.TEXT)
                .verifyStartTime(LocalTime.of(0, 0))
                .verifyEndTime(LocalTime.of(23, 59))
                .status(ChallengeStatus.ONGOING)
                .build());

        // 3. 작성자 참여 및 라운드 세팅
        UserChallenge authorJoin = userChallengeRepository.save(UserChallenge.builder()
                .user(author)
                .challenge(privateChallenge)
                .status(ChallengeJoinStatus.JOINED)
                .build());

        Round round = roundRepository.save(Round.builder()
                .challenge(privateChallenge)
                .roundNumber(1)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusWeeks(3))
                .build());
        privateChallenge.changeCurrentRound(round);

        RoundRecord record = roundRecordRepository.save(RoundRecord.builder()
                .userChallenge(authorJoin)
                .round(round)
                .build());

        // 4. 비공개 인증글 생성
        privateVerification = verificationRepository.save(Verification.createTextVerification(
                authorJoin, record, "비밀글", "내용", null, null, false, round.getId()));
    }

    @Test
    @DisplayName("비공개 챌린지 상세조회: 작성자 본인은 조회가 가능하다")
    void getDetail_Success_WhenIsMine() {
        // given
        Long verificationId = privateVerification.getId();
        Long currentUserId = author.getId();

        // when
        var result = verificationService.getVerificationDetail(verificationId, currentUserId, 0, 10);

        // then
        assertThat(result.getVerificationId()).isEqualTo(verificationId);
        assertThat(result.getIsMine()).isTrue();
    }

    @Test
    @DisplayName("비공개 챌린지 상세조회: 참여하지 않은 외부인은 예외가 발생한다")
    void getDetail_Fail_WhenStranger() {
        // given
        User stranger = userRepository.save(User.builder().nickname("stranger").userStatus(UserStatus.ACTIVE).build());

        // when & then
        assertThatThrownBy(() ->
                verificationService.getVerificationDetail(privateVerification.getId(), stranger.getId(), 0, 10)
        ).isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VERIFICATION_ACCESS_DENIED);
    }

    @Test
    @DisplayName("비공개 챌린지 상세조회: 참여 중인 다른 멤버는 조회가 가능하다")
    void getDetail_Success_WhenIsMember() {
        // given
        User member = userRepository.save(User.builder().nickname("member").userStatus(UserStatus.ACTIVE).build());
        userChallengeRepository.save(UserChallenge.builder()
                .user(member).challenge(privateChallenge).status(ChallengeJoinStatus.JOINED).build());

        // when
        var result = verificationService.getVerificationDetail(privateVerification.getId(), member.getId(), 0, 10);

        // then
        assertThat(result.getVerificationId()).isEqualTo(privateVerification.getId());
        assertThat(result.getIsMine()).isFalse();
    }

    @Test
    @DisplayName("타인 히스토리 조회: 참여하지 않은 비공개 챌린지 글은 목록에서 필터링된다")
    void getHistory_FilterPrivate() {
        // given
        User stranger = userRepository.save(User.builder().nickname("stranger").isPublic(true).userStatus(UserStatus.ACTIVE).build());

        // when
        var response = verificationService.getOtherUserVerificationHistory(author.getId(), stranger, 0, 10);

        // then
        assertThat(response.getVerifications().getContent()).isEmpty();
    }
}