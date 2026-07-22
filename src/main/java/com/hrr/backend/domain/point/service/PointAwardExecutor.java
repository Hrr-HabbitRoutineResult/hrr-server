package com.hrr.backend.domain.point.service;

import com.hrr.backend.domain.user.repository.UserRepository;
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
 *   잔액 갱신은 전달받은 User 엔티티(바깥 트랜잭션이 관리 중인 영속 객체)를 직접 건드리지 않고,
 *   userId 기준 DB 원자 UPDATE(userRepository.increasePoints)로 처리한다.
 *   이렇게 해야 point_history 저장과 user.points 증가가 이 REQUIRES_NEW 트랜잭션 안에서 함께
 *   커밋/롤백되어, 바깥 트랜잭션의 커밋 여부와 무관하게 항상 일치된 상태를 보장한다.
 *   (주의) 이 메서드는 전달받은 User 인스턴스의 in-memory 필드는 갱신하지 않는다.
 *   호출 직후 최신 포인트가 필요하면 반드시 재조회해야 한다.
 */
@Component
@RequiredArgsConstructor
public class PointAwardExecutor {

    private final PointHistoryRepository pointHistoryRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(User user, PointType type, Challenge challenge, Round round, RandomMission randomMission) {
        PointHistory history = PointHistory.of(user, type, challenge, round, randomMission);
        pointHistoryRepository.save(history);
        pointHistoryRepository.flush(); // UNIQUE 제약 위반을 이 독립 트랜잭션 안에서 즉시 확인
        userRepository.increasePoints(user.getId(), type.getPoints());
    }
}