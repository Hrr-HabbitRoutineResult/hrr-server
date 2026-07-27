package com.hrr.backend.domain.user.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserMission;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

	Optional<UserMission> findByUserAndDate(User user, LocalDate date);
    // 동시에 여러 요청이 같은 유저의 같은 날짜 UserMission을 인증 처리하려 할 때,
    // 먼저 들어온 트랜잭션이 끝날 때까지 뒤의 요청이 대기하도록 비관적 락으로 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT um FROM UserMission um WHERE um.user = :user AND um.date = :date")
    Optional<UserMission> findByUserAndDateForUpdate(@Param("user") User user, @Param("date") LocalDate date);
}
