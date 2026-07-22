package com.hrr.backend.domain.point.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrr.backend.domain.challenge.entity.Challenge;
import com.hrr.backend.domain.point.entity.PointHistory;
import com.hrr.backend.domain.point.entity.enums.PointType;
import com.hrr.backend.domain.point.repository.PointHistoryRepository;
import com.hrr.backend.domain.round.entity.Round;
import com.hrr.backend.domain.user.entity.RandomMission;
import com.hrr.backend.domain.user.entity.User;

import lombok.RequiredArgsConstructor;

/** 포인트 적립 1건을 독립된 트랜잭션(REQUIRES_NEW)으로 처리
 * - 여러 건을 순회하며 적립하는 배치성 로직에서 한 건의 실패가 앞서 처리된 건들까지 롤백시키지 않도록 격리하기 위해 사용한다.
 * - point_history의 UNIQUE 제약(챌린지/라운드 단위 중복 지급 방지) 위반으로 인한 예외가 발생해도
 *   이 트랜잭션만 롤백되고, 이 메서드를 호출한 상위 트랜잭션에는 영향을 주지 않는다.
 */
@Component
@RequiredArgsConstructor
public class PointAwardExecutor {

    private final PointHistoryRepository pointHistoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(User user, PointType type, Challenge challenge, Round round, RandomMission randomMission) {
        PointHistory history = PointHistory.of(user, type, challenge, round, randomMission);
        pointHistoryRepository.save(history);
        pointHistoryRepository.flush(); // UNIQUE 제약 위반을 이 독립 트랜잭션 안에서 즉시 확인
        user.increasePoints(type.getPoints());
    }
}