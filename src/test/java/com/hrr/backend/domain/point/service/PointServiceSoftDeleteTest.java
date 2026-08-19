package com.hrr.backend.domain.point.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.point.entity.enums.PointType;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.round.entity.RoundRecord;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserChallenge;
import com.hrr.backend.domain.verification.entity.Verification;
import com.hrr.backend.domain.verification.entity.enums.VerificationStatus;
import com.hrr.backend.domain.verification.repository.VerificationRepository;

/**
 * soft delete 전환에 따라 새로 필요해진 검증.
 *
 * 기존에는 인증글이 hard delete 되어 findById()가 null을 반환했기 때문에
 * 비동기 포인트 지급이 자연히 걸러졌다. soft delete 이후에는 행이 남아있으므로
 * isDeleted() 체크가 없으면 삭제된 인증글에 포인트가 지급된다.
 *
 * 재현 시나리오:
 *   인증글 작성 -> AFTER_COMMIT 비동기 포인트 지급이 큐에서 대기
 *   -> 그 사이 사용자가 인증글 삭제 (이 시점엔 point_history가 없어 회수할 것이 없음)
 *   -> 이후 비동기 지급이 실행되어 삭제된 인증글에 포인트가 남음
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PointServiceSoftDeleteTest {

    @InjectMocks
    private PointServiceImpl pointService;

    @Mock
    private VerificationRepository verificationRepository;

    @Mock
    private PointAwardExecutor pointAwardExecutor;

    private Verification createVerification(Long id, VerificationStatus status) {
        User user = User.builder().id(1L).nickname("tester").build();
        Challenge challenge = Challenge.builder().id(100L).title("Test Challenge").build();
        Round round = Round.builder().id(200L).challenge(challenge).build();
        UserChallenge userChallenge = UserChallenge.builder().user(user).challenge(challenge).build();
        RoundRecord roundRecord = RoundRecord.builder().round(round).userChallenge(userChallenge).build();

        return Verification.builder()
                .id(id)
                .roundRecord(roundRecord)
                .userChallenge(userChallenge)
                .status(status)
                .build();
    }

    @Test
    @DisplayName("삭제된(DELETED) 인증글에는 비동기 포인트가 지급되지 않는다")
    void awardVerificationTriggeredPoints_whenDeleted_skipsAward() {
        // given
        Long verificationId = 1L;
        Verification deleted = createVerification(verificationId, VerificationStatus.COMPLETED);
        deleted.softDelete(LocalDateTime.now());

        given(verificationRepository.findById(verificationId)).willReturn(Optional.of(deleted));

        // when
        pointService.awardVerificationTriggeredPoints(verificationId);

        // then: 포인트 적립 실행기가 단 한 번도 호출되지 않아야 한다
        verify(pointAwardExecutor, never())
                .execute(any(User.class), any(PointType.class), any(Challenge.class), any(Round.class), any(), any());
    }

    @Test
    @DisplayName("인증글이 존재하지 않으면 비동기 포인트 지급을 건너뛴다")
    void awardVerificationTriggeredPoints_whenNotFound_skipsAward() {
        // given
        Long verificationId = 999L;
        given(verificationRepository.findById(verificationId)).willReturn(Optional.empty());

        // when
        pointService.awardVerificationTriggeredPoints(verificationId);

        // then
        verify(pointAwardExecutor, never())
                .execute(any(User.class), any(PointType.class), any(Challenge.class), any(Round.class), any(), any());
    }
}