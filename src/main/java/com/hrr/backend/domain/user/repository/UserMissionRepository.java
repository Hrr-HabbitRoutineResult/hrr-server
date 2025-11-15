package com.hrr.backend.domain.user.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hrr.backend.domain.user.entity.User;
import com.hrr.backend.domain.user.entity.UserMission;

public interface UserMissionRepository extends JpaRepository<UserMission, Long> {

	Optional<UserMission> findByUserAndDate(User user, LocalDate date);
}
