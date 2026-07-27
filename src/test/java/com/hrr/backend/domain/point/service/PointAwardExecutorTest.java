package com.hrr.backend.domain.point.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.point.entity.PointHistory;
import com.hrr.backend.domain.point.entity.enums.PointType;
import com.hrr.backend.domain.point.repository.PointHistoryRepository;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PointAwardExecutorTest {

    @InjectMocks
    private PointAwardExecutor pointAwardExecutor;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("execute는 포인트 내역을 저장/flush한 뒤, DB 원자 UPDATE로 유저 포인트를 증가시킨다 (엔티티 필드는 직접 건드리지 않음)")
    void execute_savesHistoryAndAtomicallyUpdatesUserPointsViaDb() {
        // given
        User user = User.builder().id(1L).points(0L).build();
        Challenge challenge = Challenge.builder().id(10L).build();

        given(pointHistoryRepository.save(any(PointHistory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when (verification은 이 케이스에서 null - FIRST_VERIFICATION이지만 연결 인증글 없이 호출된 예시)
        pointAwardExecutor.execute(user, PointType.FIRST_VERIFICATION, challenge, null, null, null);

        // then: 저장 -> flush -> DB 원자 증가 순서로 호출되어야 함
        InOrder inOrder = Mockito.inOrder(pointHistoryRepository, userRepository);
        inOrder.verify(pointHistoryRepository, times(1)).save(any(PointHistory.class));
        inOrder.verify(pointHistoryRepository, times(1)).flush();
        inOrder.verify(userRepository, times(1)).increasePoints(1L, 1L);

        // 엔티티의 in-memory 필드는 이 메서드 안에서 직접 건드리지 않는다 (DB 원자 UPDATE로만 반영)
        verify(userRepository, times(1)).increasePoints(user.getId(), (long) PointType.FIRST_VERIFICATION.getPoints());
    }
}